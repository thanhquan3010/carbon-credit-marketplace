package com.carbonmarketplace.transactionservice.service;

import com.carbonmarketplace.transactionservice.client.PaymentServiceClient;
import com.carbonmarketplace.transactionservice.dto.TransactionDTOs.PayoutRequest;
import com.carbonmarketplace.transactionservice.dto.TransactionDTOs.PayoutResult;
import com.carbonmarketplace.transactionservice.entity.SettlementBatch;
import com.carbonmarketplace.transactionservice.entity.SettlementBatch.BatchStatus;
import com.carbonmarketplace.transactionservice.entity.Transaction;
import com.carbonmarketplace.transactionservice.exception.SettlementException;
import com.carbonmarketplace.transactionservice.repository.SettlementBatchRepository;
import com.carbonmarketplace.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Settlement Service
 * 
 * Handles T+2 settlement processing, batch operations, and payout management.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementBatchRepository settlementBatchRepository;
    private final TransactionRepository transactionRepository;
    private final EscrowService escrowService;
    private final PaymentServiceClient paymentServiceClient;

    @Value("${transaction.settlement.batch-size:100}")
    private Integer batchSize;

    @Value("${transaction.settlement.processing-threads:5}")
    private Integer processingThreads;

    @Value("${transaction.settlement.t2-enabled:true}")
    private Boolean t2Enabled;

    @Value("${transaction.settlement.cut-off-hour:15}")
    private Integer cutOffHour;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Create a new settlement batch
     */
    public SettlementBatch createSettlementBatch(LocalDate settlementDate) {
        log.info("Creating settlement batch for date: {}", settlementDate);

        // Check if batch already exists
        if (settlementBatchRepository.existsBatchForDate(settlementDate)) {
            throw new SettlementException("Settlement batch already exists for date: " +
                    settlementDate);
        }

        SettlementBatch batch = SettlementBatch.builder()
                .settlementDate(settlementDate)
                .status(BatchStatus.PENDING)
                .cutoffTime(LocalDateTime.of(settlementDate,
                        java.time.LocalTime.of(cutOffHour, 0)))
                .build();

        batch.generateBatchNumber();

        // Collect transactions for the batch
        LocalDateTime settlementDateTime = LocalDateTime.of(settlementDate,
                java.time.LocalTime.of(cutOffHour + 2, 0));
        List<Transaction> transactions = transactionRepository.findTransactionsForSettlement(
                Transaction.TransactionStatus.IN_SETTLEMENT,
                settlementDateTime);

        log.info("Found {} transactions for settlement batch", transactions.size());

        // Assign transactions to batch
        if (!transactions.isEmpty()) {
            List<UUID> transactionIds = transactions.stream()
                    .map(Transaction::getTransactionId)
                    .collect(Collectors.toList());

            transactionRepository.assignToSettlementBatch(batch.getBatchId(), transactionIds);

            batch.setTransactions(transactions);
            batch.calculateStatistics();

            // Check if batch requires approval
            if (batch.requiresManualApproval()) {
                batch.setRequiresApproval(true);
                batch.setStatus(BatchStatus.AWAITING_APPROVAL);
                log.warn("Settlement batch {} requires manual approval", batch.getBatchNumber());
            }
        }

        return settlementBatchRepository.save(batch);
    }

    /**
     * Process settlement batch
     */
    @Async
    public CompletableFuture<BatchProcessingResult> processSettlementBatch(UUID batchId) {
        log.info("Processing settlement batch: {}", batchId);

        SettlementBatch batch = settlementBatchRepository.findById(batchId)
                .orElseThrow(() -> new SettlementException("Batch not found: " + batchId));

        if (!batch.canProcess()) {
            throw new SettlementException("Batch cannot be processed. Status: " +
                    batch.getStatus());
        }

        batch.setStatus(BatchStatus.PROCESSING);
        batch.setProcessingStartedAt(LocalDateTime.now());
        settlementBatchRepository.save(batch);

        // Process transactions in parallel
        List<Transaction> transactions = transactionRepository.findBySettlementBatchId(batchId);

        int successCount = 0;
        int failureCount = 0;
        BigDecimal totalProcessed = BigDecimal.ZERO;

        // Process in chunks
        for (int i = 0; i < transactions.size(); i += batchSize) {
            int end = Math.min(i + batchSize, transactions.size());
            List<Transaction> chunk = transactions.subList(i, end);

            List<CompletableFuture<Boolean>> futures = chunk.stream()
                    .map(transaction -> processTransaction(transaction))
                    .collect(Collectors.toList());

            // Wait for chunk completion
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Count results
            for (int j = 0; j < futures.size(); j++) {
                try {
                    if (futures.get(j).get()) {
                        successCount++;
                        totalProcessed = totalProcessed.add(chunk.get(j).getSellerReceivesVnd());
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to process transaction in batch", e);
                }
            }
        }

        // Update batch status
        batch.setSuccessfulTransactions(successCount);
        batch.setFailedTransactions(failureCount);
        batch.setProcessingCompletedAt(LocalDateTime.now());
        batch.recordProcessingTime();

        if (failureCount == 0) {
            batch.setStatus(BatchStatus.COMPLETED);
        } else if (successCount > 0) {
            batch.setStatus(BatchStatus.PARTIALLY_COMPLETED);
        } else {
            batch.setStatus(BatchStatus.FAILED);
        }

        settlementBatchRepository.save(batch);

        log.info("Settlement batch {} completed. Success: {}, Failed: {}",
                batch.getBatchNumber(), successCount, failureCount);

        return CompletableFuture.completedFuture(
                new BatchProcessingResult(batchId, successCount, failureCount, totalProcessed));
    }

    /**
     * Process individual transaction settlement
     */
    @Async
    private CompletableFuture<Boolean> processTransaction(Transaction transaction) {
        log.debug("Processing settlement for transaction: {}", transaction.getTransactionId());

        try {
            // 1. Release escrow funds
            if (transaction.getEscrowAccount() != null) {
                escrowService.releaseFunds(
                        transaction.getEscrowAccount().getEscrowAccountId(),
                        "SETTLEMENT");
            }

            // 2. Process payout to seller
            PayoutResult payout = processPayout(transaction);

            // 3. Update transaction status
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setSettlementCompletedAt(LocalDateTime.now());
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            log.info("Settlement completed for transaction: {}", transaction.getTransactionId());
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Settlement failed for transaction: {}", transaction.getTransactionId(), e);

            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason("Settlement failed: " + e.getMessage());
            transaction.setFailedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Process payout to seller
     */
    private PayoutResult processPayout(Transaction transaction) {
        log.debug("Processing payout for transaction: {}", transaction.getTransactionId());

        PayoutRequest request = PayoutRequest.builder()
                .transactionId(transaction.getTransactionId())
                .paymentId(transaction.getPaymentId())
                .sellerId(transaction.getSellerId())
                .amount(transaction.getSellerReceivesVnd())
                .currency("VND")
                .description("Carbon credit sale payout - " + transaction.getTransactionId())
                .build();

        return paymentServiceClient.processPayout(request);
    }

    /**
     * Approve settlement batch
     */
    public void approveSettlementBatch(UUID batchId, UUID approvedBy, String notes) {
        log.info("Approving settlement batch: {}", batchId);

        SettlementBatch batch = settlementBatchRepository.findById(batchId)
                .orElseThrow(() -> new SettlementException("Batch not found: " + batchId));

        if (batch.getStatus() != BatchStatus.AWAITING_APPROVAL) {
            throw new SettlementException("Batch is not awaiting approval. Status: " +
                    batch.getStatus());
        }

        batch.setStatus(BatchStatus.APPROVED);
        batch.setApprovedBy(approvedBy);
        batch.setApprovedAt(LocalDateTime.now());
        batch.setApprovalNotes(notes);

        settlementBatchRepository.save(batch);

        // Trigger processing
        processSettlementBatch(batchId);
    }

    /**
     * Reject settlement batch
     */
    public void rejectSettlementBatch(UUID batchId, UUID rejectedBy, String reason) {
        log.info("Rejecting settlement batch: {}", batchId);

        SettlementBatch batch = settlementBatchRepository.findById(batchId)
                .orElseThrow(() -> new SettlementException("Batch not found: " + batchId));

        batch.setStatus(BatchStatus.CANCELLED);
        batch.setNotes("Rejected: " + reason);

        // Release transactions from batch
        List<Transaction> transactions = transactionRepository.findBySettlementBatchId(batchId);
        transactions.forEach(transaction -> {
            transaction.setSettlementBatchId(null);
            transaction.setStatus(Transaction.TransactionStatus.IN_SETTLEMENT);
            transactionRepository.save(transaction);
        });

        settlementBatchRepository.save(batch);
    }

    /**
     * Reconcile settlement batch
     */
    public void reconcileSettlementBatch(UUID batchId, UUID reconciledBy, String notes) {
        log.info("Reconciling settlement batch: {}", batchId);

        SettlementBatch batch = settlementBatchRepository.findById(batchId)
                .orElseThrow(() -> new SettlementException("Batch not found: " + batchId));

        if (batch.getStatus() != BatchStatus.COMPLETED &&
                batch.getStatus() != BatchStatus.PARTIALLY_COMPLETED) {
            throw new SettlementException("Batch cannot be reconciled. Status: " +
                    batch.getStatus());
        }

        batch.setReconciled(true);
        batch.setReconciledAt(LocalDateTime.now());
        batch.setReconciledBy(reconciledBy);
        batch.setReconciliationNotes(notes);
        batch.setStatus(BatchStatus.RECONCILED);

        settlementBatchRepository.save(batch);
    }

    /**
     * Scheduled task to create daily settlement batches
     */
    @Scheduled(cron = "0 0 16 * * MON-FRI") // 4 PM on weekdays
    public void createDailySettlementBatch() {
        if (!t2Enabled) {
            log.debug("T+2 settlement is disabled");
            return;
        }

        LocalDate settlementDate = LocalDate.now();

        // Skip if batch already exists
        if (settlementBatchRepository.existsBatchForDate(settlementDate)) {
            log.debug("Settlement batch already exists for date: {}", settlementDate);
            return;
        }

        try {
            SettlementBatch batch = createSettlementBatch(settlementDate);
            log.info("Created daily settlement batch: {}", batch.getBatchNumber());

            // Auto-process if no approval required
            if (!batch.getRequiresApproval() && batch.getTotalTransactions() > 0) {
                processSettlementBatch(batch.getBatchId());
            }
        } catch (Exception e) {
            log.error("Failed to create daily settlement batch", e);
        }
    }

    /**
     * Scheduled task to process approved batches
     */
    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    public void processApprovedBatches() {
        log.debug("Checking for approved settlement batches");

        List<SettlementBatch> approvedBatches = settlementBatchRepository
                .findBatchesReadyForProcessing(LocalDateTime.now());

        for (SettlementBatch batch : approvedBatches) {
            try {
                if (batch.getStatus() == BatchStatus.APPROVED ||
                        (batch.getStatus() == BatchStatus.PENDING && !batch.getRequiresApproval())) {
                    processSettlementBatch(batch.getBatchId());
                }
            } catch (Exception e) {
                log.error("Failed to process approved batch: {}", batch.getBatchNumber(), e);
            }
        }
    }

    /**
     * Get settlement statistics
     */
    public SettlementStatistics getSettlementStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> stats = settlementBatchRepository.getBatchStatistics(startDate, endDate);

        if (!stats.isEmpty() && stats.get(0) != null) {
            Object[] row = stats.get(0);
            return SettlementStatistics.builder()
                    .totalBatches((Long) row[0])
                    .totalTransactions((Long) row[1])
                    .totalAmount((BigDecimal) row[2])
                    .totalFees((BigDecimal) row[3])
                    .totalPayouts((BigDecimal) row[4])
                    .build();
        }

        return SettlementStatistics.builder()
                .totalBatches(0L)
                .totalTransactions(0L)
                .totalAmount(BigDecimal.ZERO)
                .totalFees(BigDecimal.ZERO)
                .totalPayouts(BigDecimal.ZERO)
                .build();
    }

    /**
     * Get pending settlements
     */
    public Page<SettlementBatch> getPendingSettlements(int page, int size) {
        return settlementBatchRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(BatchStatus.PENDING, BatchStatus.AWAITING_APPROVAL, BatchStatus.PROCESSING),
                PageRequest.of(page, size));
    }

    /**
     * Retry failed transactions in batch
     */
    public void retryFailedTransactions(UUID batchId) {
        log.info("Retrying failed transactions in batch: {}", batchId);

        List<Transaction> failedTransactions = transactionRepository
                .findBySettlementBatchId(batchId).stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.FAILED)
                .collect(Collectors.toList());

        for (Transaction transaction : failedTransactions) {
            try {
                transaction.setStatus(Transaction.TransactionStatus.IN_SETTLEMENT);
                transaction.setRetryCount(transaction.getRetryCount() + 1);
                transactionRepository.save(transaction);

                processTransaction(transaction);
            } catch (Exception e) {
                log.error("Retry failed for transaction: {}", transaction.getTransactionId(), e);
            }
        }
    }

    // Inner classes for results
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class BatchProcessingResult {
        private UUID batchId;
        private int successCount;
        private int failureCount;
        private BigDecimal totalProcessed;
    }

    @lombok.Data
    @lombok.Builder
    public static class SettlementStatistics {
        private Long totalBatches;
        private Long totalTransactions;
        private BigDecimal totalAmount;
        private BigDecimal totalFees;
        private BigDecimal totalPayouts;
    }
}
