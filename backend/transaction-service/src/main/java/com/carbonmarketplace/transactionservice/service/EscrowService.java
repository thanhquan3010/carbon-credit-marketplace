package com.carbonmarketplace.transactionservice.service;

import com.carbonmarketplace.transactionservice.dto.SettlementScheduleResult;
import com.carbonmarketplace.transactionservice.entity.EscrowAccount;
import com.carbonmarketplace.transactionservice.entity.EscrowAccount.EscrowStatus;
import com.carbonmarketplace.transactionservice.entity.SettlementBatch;
import com.carbonmarketplace.transactionservice.entity.Transaction;
import com.carbonmarketplace.transactionservice.exception.EscrowException;
import com.carbonmarketplace.transactionservice.repository.EscrowAccountRepository;
import com.carbonmarketplace.transactionservice.repository.SettlementBatchRepository;
import com.carbonmarketplace.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Escrow Service
 * 
 * Manages escrow accounts, fund holding, and T+2 settlement processing.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EscrowService {

    private final EscrowAccountRepository escrowAccountRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final RedissonClient redissonClient;
    
    @Value("${transaction.escrow.hold-days:2}")
    private Integer holdDays;
    
    @Value("${transaction.escrow.auto-release-enabled:true}")
    private Boolean autoReleaseEnabled;
    
    @Value("${transaction.settlement.cut-off-hour:15}")
    private Integer cutOffHour;
    
    @Value("${transaction.settlement.batch-size:100}")
    private Integer batchSize;

    /**
     * Create a new escrow account for a transaction
     */
    public EscrowAccount createEscrowAccount(Transaction transaction, String creditLockId) {
        log.debug("Creating escrow account for transaction: {}", transaction.getTransactionId());
        
        // Check if escrow already exists
        escrowAccountRepository.findByTransactionTransactionId(transaction.getTransactionId())
            .ifPresent(existing -> {
                throw new EscrowException("Escrow account already exists: " + 
                                        existing.getAccountNumber());
            });
        
        EscrowAccount escrow = EscrowAccount.builder()
                .transaction(transaction)
                .amountVnd(transaction.getTotalAmountVnd())
                .creditAmountTons(transaction.getCreditAmountTons())
                .status(EscrowStatus.PENDING)
                .creditsLockId(creditLockId)
                .autoReleaseEnabled(autoReleaseEnabled && !transaction.getIsExpressSettlement())
                .build();
        
        // Generate unique account number
        escrow.generateAccountNumber();
        
        // Calculate T+2 settlement date
        LocalDateTime scheduledRelease = calculateSettlementDate(
            LocalDateTime.now(), 
            holdDays,
            transaction.getIsExpressSettlement()
        );
        escrow.setScheduledReleaseDate(scheduledRelease);
        
        // Set hold expiry (30 days)
        escrow.setHoldExpiresAt(LocalDateTime.now().plusDays(30));
        
        return escrowAccountRepository.save(escrow);
    }

    /**
     * Hold funds in escrow account
     */
    public void holdFunds(UUID escrowAccountId, UUID paymentId) {
        log.debug("Holding funds in escrow account: {}", escrowAccountId);
        
        EscrowAccount escrow = escrowAccountRepository.findByIdWithLock(escrowAccountId)
            .orElseThrow(() -> new EscrowException("Escrow account not found: " + escrowAccountId));
        
        if (escrow.getStatus() != EscrowStatus.PENDING && 
            escrow.getStatus() != EscrowStatus.AWAITING_FUNDS) {
            throw new EscrowException("Invalid escrow status for holding funds: " + 
                                    escrow.getStatus());
        }
        
        escrow.setStatus(EscrowStatus.HELD);
        escrow.setHoldPlacedAt(LocalDateTime.now());
        escrow.setCreditsLocked(true);
        escrow.setLockedBy("PAYMENT_" + paymentId);
        
        escrowAccountRepository.save(escrow);
        
        // Update transaction escrow status
        Transaction transaction = escrow.getTransaction();
        transaction.setEscrowStatus(Transaction.EscrowStatus.HELD);
        transactionRepository.save(transaction);
        
        log.info("Funds held in escrow account: {} for amount: {} VND", 
                escrow.getAccountNumber(), escrow.getAmountVnd());
    }

    /**
     * Release funds from escrow
     */
    public void releaseFunds(UUID escrowAccountId, String releaseType) {
        log.debug("Releasing funds from escrow account: {}", escrowAccountId);
        
        String lockKey = "escrow:release:" + escrowAccountId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                EscrowAccount escrow = escrowAccountRepository.findByIdWithLock(escrowAccountId)
                    .orElseThrow(() -> new EscrowException("Escrow account not found: " + 
                                                         escrowAccountId));
                
                if (!escrow.canRelease() && !"EARLY".equals(releaseType)) {
                    throw new EscrowException("Escrow not ready for release. Status: " + 
                                           escrow.getStatus() + ", Release date: " + 
                                           escrow.getScheduledReleaseDate());
                }
                
                escrow.setStatus(EscrowStatus.RELEASED);
                escrow.setActualReleaseDate(LocalDateTime.now());
                escrow.setReleaseType(releaseType);
                escrow.setReleasedBy("SYSTEM_" + releaseType);
                
                escrowAccountRepository.save(escrow);
                
                // Update transaction
                Transaction transaction = escrow.getTransaction();
                transaction.setEscrowStatus(Transaction.EscrowStatus.RELEASED);
                transaction.setSettlementCompletedAt(LocalDateTime.now());
                
                if (transaction.getStatus() == Transaction.TransactionStatus.IN_SETTLEMENT) {
                    transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                }
                
                transactionRepository.save(transaction);
                
                log.info("Funds released from escrow: {} to seller: {}", 
                        escrow.getAccountNumber(), transaction.getSellerId());
            } else {
                throw new EscrowException("Could not acquire lock for escrow release");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EscrowException("Escrow release interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Refund escrow to buyer
     */
    public void refundEscrow(UUID escrowAccountId, String refundReason, BigDecimal refundAmount) {
        log.debug("Refunding escrow account: {}", escrowAccountId);
        
        EscrowAccount escrow = escrowAccountRepository.findByIdWithLock(escrowAccountId)
            .orElseThrow(() -> new EscrowException("Escrow account not found: " + escrowAccountId));
        
        if (!escrow.canRefund()) {
            throw new EscrowException("Cannot refund escrow in status: " + escrow.getStatus());
        }
        
        escrow.setStatus(EscrowStatus.REFUNDED);
        escrow.setRefundAmountVnd(refundAmount);
        escrow.setRefundDate(LocalDateTime.now());
        escrow.setRefundReason(refundReason);
        escrow.setRefundReference("REF-" + UUID.randomUUID().toString().substring(0, 8));
        
        escrowAccountRepository.save(escrow);
        
        // Update transaction
        Transaction transaction = escrow.getTransaction();
        transaction.setEscrowStatus(Transaction.EscrowStatus.REFUNDED);
        transaction.setStatus(Transaction.TransactionStatus.REFUNDED);
        transaction.setRefundAmountVnd(refundAmount);
        transaction.setRefundReason(refundReason);
        
        transactionRepository.save(transaction);
        
        log.info("Escrow refunded: {} for amount: {} VND", 
                escrow.getAccountNumber(), refundAmount);
    }

    /**
     * Cancel escrow account
     */
    public void cancelEscrowAccount(UUID escrowAccountId) {
        log.debug("Cancelling escrow account: {}", escrowAccountId);
        
        escrowAccountRepository.findById(escrowAccountId)
            .ifPresent(escrow -> {
                escrow.setStatus(EscrowStatus.CANCELLED);
                escrowAccountRepository.save(escrow);
                
                // Unlock credits if locked
                if (escrow.getCreditsLocked()) {
                    escrow.setCreditsLocked(false);
                    escrowAccountRepository.save(escrow);
                }
            });
    }

    /**
     * Schedule settlement for T+2
     */
    public SettlementScheduleResult scheduleSettlement(Transaction transaction, 
                                                       EscrowAccount escrow) {
        log.debug("Scheduling settlement for transaction: {}", transaction.getTransactionId());
        
        LocalDateTime settlementDate = escrow.getScheduledReleaseDate();
        LocalDate batchDate = settlementDate.toLocalDate();
        
        // Find or create settlement batch for the date
        SettlementBatch batch = settlementBatchRepository.findBySettlementDate(batchDate)
            .stream()
            .filter(b -> b.getStatus() != SettlementBatch.BatchStatus.CANCELLED &&
                        b.getStatus() != SettlementBatch.BatchStatus.FAILED)
            .findFirst()
            .orElseGet(() -> createSettlementBatch(batchDate));
        
        // Add transaction to batch
        transaction.setSettlementBatchId(batch.getBatchId());
        transaction.setSettlementDate(settlementDate);
        transactionRepository.save(transaction);
        
        // Update batch statistics
        batch.setTotalTransactions(batch.getTotalTransactions() + 1);
        batch.setTotalAmountVnd(batch.getTotalAmountVnd().add(transaction.getTotalAmountVnd()));
        batch.setTotalFeesVnd(batch.getTotalFeesVnd().add(transaction.getPlatformFeeVnd()));
        batch.setTotalPayoutsVnd(batch.getTotalPayoutsVnd().add(transaction.getSellerReceivesVnd()));
        
        // Check if batch requires approval
        if (batch.requiresManualApproval()) {
            batch.setRequiresApproval(true);
            batch.setStatus(SettlementBatch.BatchStatus.AWAITING_APPROVAL);
        }
        
        settlementBatchRepository.save(batch);
        
        return SettlementScheduleResult.builder()
                .settlementId(batch.getBatchId())
                .scheduledDate(settlementDate)
                .batchNumber(batch.getBatchNumber())
                .build();
    }

    /**
     * Cancel scheduled settlement
     */
    public void cancelSettlement(UUID settlementId) {
        log.debug("Cancelling settlement: {}", settlementId);
        
        settlementBatchRepository.findById(settlementId)
            .ifPresent(batch -> {
                // Remove transaction from batch
                List<Transaction> transactions = transactionRepository
                    .findBySettlementBatchId(settlementId);
                
                transactions.forEach(transaction -> {
                    transaction.setSettlementBatchId(null);
                    transaction.setSettlementDate(null);
                    transactionRepository.save(transaction);
                });
                
                // Update batch statistics
                batch.calculateStatistics();
                settlementBatchRepository.save(batch);
            });
    }

    /**
     * Create a new settlement batch
     */
    private SettlementBatch createSettlementBatch(LocalDate settlementDate) {
        SettlementBatch batch = SettlementBatch.builder()
                .settlementDate(settlementDate)
                .status(SettlementBatch.BatchStatus.PENDING)
                .cutoffTime(LocalDateTime.of(settlementDate, LocalTime.of(cutOffHour, 0)))
                .build();
        
        batch.generateBatchNumber();
        
        return settlementBatchRepository.save(batch);
    }

    /**
     * Calculate T+2 settlement date
     */
    private LocalDateTime calculateSettlementDate(LocalDateTime baseDate, Integer days, 
                                                  Boolean isExpress) {
        if (isExpress) {
            // Express settlement - same day if before cutoff, next day otherwise
            LocalTime cutOff = LocalTime.of(cutOffHour, 0);
            if (baseDate.toLocalTime().isBefore(cutOff)) {
                return LocalDateTime.of(baseDate.toLocalDate(), cutOff.plusHours(2));
            } else {
                return LocalDateTime.of(baseDate.toLocalDate().plusDays(1), cutOff.plusHours(2));
            }
        }
        
        // Standard T+2 settlement
        LocalDateTime settlementDate = baseDate.plusDays(days);
        
        // Skip weekends
        while (settlementDate.getDayOfWeek().getValue() > 5) {
            settlementDate = settlementDate.plusDays(1);
        }
        
        // Set to cutoff time + 2 hours for processing
        return LocalDateTime.of(settlementDate.toLocalDate(), 
                              LocalTime.of(cutOffHour + 2, 0));
    }

    /**
     * Process automatic escrow releases (scheduled task)
     */
    @Scheduled(cron = "${transaction.escrow.release-schedule-cron:0 0 2 * * ?}")
    public void processAutoReleases() {
        if (!autoReleaseEnabled) {
            log.debug("Auto-release is disabled");
            return;
        }
        
        log.info("Starting automatic escrow release processing");
        
        LocalDateTime now = LocalDateTime.now();
        List<EscrowAccount> accountsToRelease = escrowAccountRepository
            .findAccountsForAutoRelease(now);
        
        log.info("Found {} escrow accounts ready for auto-release", accountsToRelease.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (EscrowAccount escrow : accountsToRelease) {
            try {
                releaseFunds(escrow.getEscrowAccountId(), "AUTO");
                successCount++;
            } catch (Exception e) {
                log.error("Failed to auto-release escrow: {}", escrow.getAccountNumber(), e);
                failureCount++;
            }
        }
        
        log.info("Auto-release completed. Success: {}, Failures: {}", successCount, failureCount);
    }

    /**
     * Process expired escrow holds (scheduled task)
     */
    @Scheduled(cron = "0 30 3 * * ?") // Daily at 3:30 AM
    public void processExpiredHolds() {
        log.info("Processing expired escrow holds");
        
        List<EscrowAccount> expiredHolds = escrowAccountRepository
            .findExpiredHolds(LocalDateTime.now());
        
        for (EscrowAccount escrow : expiredHolds) {
            try {
                escrow.setStatus(EscrowStatus.EXPIRED);
                escrowAccountRepository.save(escrow);
                
                // Update transaction
                Transaction transaction = escrow.getTransaction();
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setFailureReason("Escrow hold expired");
                transaction.setFailedAt(LocalDateTime.now());
                transactionRepository.save(transaction);
                
                log.warn("Escrow hold expired: {}", escrow.getAccountNumber());
            } catch (Exception e) {
                log.error("Failed to process expired escrow: {}", escrow.getAccountNumber(), e);
            }
        }
    }

    /**
     * Get escrow statistics
     */
    public EscrowStatistics getEscrowStatistics() {
        Long activeEscrows = escrowAccountRepository.countActiveEscrows();
        
        // Additional statistics can be calculated here
        
        return EscrowStatistics.builder()
                .activeEscrows(activeEscrows)
                .build();
    }
    
    /**
     * Early release with approval
     */
    public void earlyRelease(UUID escrowAccountId, UUID approvedBy, String reason) {
        log.info("Processing early release for escrow: {}", escrowAccountId);
        
        EscrowAccount escrow = escrowAccountRepository.findByIdWithLock(escrowAccountId)
            .orElseThrow(() -> new EscrowException("Escrow account not found: " + escrowAccountId));
        
        if (escrow.getStatus() != EscrowStatus.HELD) {
            throw new EscrowException("Cannot early release escrow in status: " + 
                                    escrow.getStatus());
        }
        
        escrow.setEarlyReleaseReason(reason);
        escrow.setEarlyReleaseApprovedBy(approvedBy);
        escrowAccountRepository.save(escrow);
        
        releaseFunds(escrowAccountId, "EARLY");
    }
    
    // Inner class for statistics
    @lombok.Data
    @lombok.Builder
    public static class EscrowStatistics {
        private Long activeEscrows;
        private BigDecimal totalHeldAmount;
        private Long pendingReleases;
        private Long expiredHolds;
    }
}
