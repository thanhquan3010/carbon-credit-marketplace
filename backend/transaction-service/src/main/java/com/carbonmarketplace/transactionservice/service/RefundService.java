package com.carbonmarketplace.transactionservice.service;

import com.carbonmarketplace.transactionservice.client.CarbonCreditServiceClient;
import com.carbonmarketplace.transactionservice.client.PaymentServiceClient;
import com.carbonmarketplace.transactionservice.dto.RefundCreditRequest;
import com.carbonmarketplace.transactionservice.dto.RefundPaymentRequest;
import com.carbonmarketplace.transactionservice.entity.*;
import com.carbonmarketplace.transactionservice.exception.RefundException;
import com.carbonmarketplace.transactionservice.repository.RefundRequestRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Refund Service
 * 
 * Manages refund requests, approval workflows, and refund processing.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final TransactionRepository transactionRepository;
    private final EscrowService escrowService;
    private final PaymentServiceClient paymentServiceClient;
    private final CarbonCreditServiceClient carbonCreditServiceClient;
    
    @Value("${transaction.refund.auto-approve-threshold-vnd:1000000}")
    private BigDecimal autoApproveThreshold;
    
    @Value("${transaction.refund.approval-required-above:10000000}")
    private BigDecimal manualApprovalThreshold;
    
    @Value("${transaction.refund.max-refund-days:30}")
    private Integer maxRefundDays;
    
    @Value("${transaction.fees.platform-percentage:5.0}")
    private BigDecimal platformFeePercentage;

    /**
     * Create a refund request
     */
    public RefundRequest createRefundRequest(RefundRequestDto request) {
        log.info("Creating refund request for transaction: {}", request.getTransactionId());
        
        // Validate transaction
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RefundException("Transaction not found: " + 
                                                      request.getTransactionId()));
        
        // Check if transaction is refundable
        if (!transaction.isRefundable()) {
            throw new RefundException("Transaction is not refundable. Status: " + 
                                    transaction.getStatus() + ", Completed: " + 
                                    transaction.getCompletedAt());
        }
        
        // Check if refund already exists
        if (refundRequestRepository.hasActiveRefundRequest(request.getTransactionId())) {
            throw new RefundException("Active refund request already exists for transaction");
        }
        
        // Check refund amount
        BigDecimal totalRefunded = refundRequestRepository
                .calculateTotalRefundedAmount(request.getTransactionId());
        BigDecimal remainingRefundable = transaction.getTotalAmountVnd().subtract(totalRefunded);
        
        if (request.getRefundAmount().compareTo(remainingRefundable) > 0) {
            throw new RefundException("Refund amount exceeds remaining refundable amount: " + 
                                    remainingRefundable);
        }
        
        // Create refund request
        RefundRequest refund = RefundRequest.builder()
                .transactionId(request.getTransactionId())
                .originalPaymentId(transaction.getPaymentId())
                .refundAmountVnd(request.getRefundAmount())
                .originalAmountVnd(transaction.getTotalAmountVnd())
                .refundType(determineRefundType(request.getRefundAmount(), 
                                               transaction.getTotalAmountVnd()))
                .status(RefundRequest.RefundStatus.PENDING)
                .requesterId(request.getRequesterId())
                .requesterType(request.getRequesterType())
                .reason(request.getReason())
                .reasonCategory(request.getReasonCategory())
                .supportingDocuments(request.getSupportingDocuments())
                .requestedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        
        refund.generateRefundNumber();
        refund.calculateFinancialImpact(platformFeePercentage);
        
        // Check if auto-approval is possible
        if (refund.canAutoApprove(autoApproveThreshold)) {
            refund.setRequiresApproval(false);
            refund.setAutoApproved(true);
            refund.setStatus(RefundRequest.RefundStatus.APPROVED);
            refund.setApprovedAt(LocalDateTime.now());
            refund.setApprovalNotes("Auto-approved: Amount below threshold");
            
            // Trigger processing
            processRefundAsync(refund);
        } else {
            refund.setRequiresApproval(true);
            refund.setStatus(RefundRequest.RefundStatus.AWAITING_APPROVAL);
        }
        
        return refundRequestRepository.save(refund);
    }

    /**
     * Approve refund request
     */
    public void approveRefund(UUID refundId, UUID approvedBy, String notes) {
        log.info("Approving refund request: {}", refundId);
        
        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new RefundException("Refund request not found: " + refundId));
        
        if (refund.getStatus() != RefundRequest.RefundStatus.AWAITING_APPROVAL) {
            throw new RefundException("Refund is not awaiting approval. Status: " + 
                                    refund.getStatus());
        }
        
        if (refund.isExpired()) {
            throw new RefundException("Refund request has expired");
        }
        
        refundRequestRepository.approveRefund(
            refundId, 
            RefundRequest.RefundStatus.APPROVED,
            approvedBy, 
            LocalDateTime.now()
        );
        
        refund.setStatus(RefundRequest.RefundStatus.APPROVED);
        refund.setApprovedBy(approvedBy);
        refund.setApprovedAt(LocalDateTime.now());
        refund.setApprovalNotes(notes);
        
        // Trigger processing
        processRefundAsync(refund);
    }

    /**
     * Reject refund request
     */
    public void rejectRefund(UUID refundId, UUID rejectedBy, String reason) {
        log.info("Rejecting refund request: {}", refundId);
        
        refundRequestRepository.rejectRefund(
            refundId, 
            rejectedBy, 
            LocalDateTime.now(), 
            reason
        );
    }

    /**
     * Process approved refund asynchronously
     */
    @Async
    public CompletableFuture<RefundProcessingResult> processRefundAsync(RefundRequest refund) {
        log.info("Processing refund: {}", refund.getRefundNumber());
        
        try {
            RefundProcessingResult result = processRefund(refund);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Refund processing failed: {}", refund.getRefundNumber(), e);
            
            refund.setStatus(RefundRequest.RefundStatus.FAILED);
            refund.setFailureReason(e.getMessage());
            refund.setRetryCount(refund.getRetryCount() + 1);
            refund.setLastRetryAt(LocalDateTime.now());
            refundRequestRepository.save(refund);
            
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Process refund synchronously
     */
    private RefundProcessingResult processRefund(RefundRequest refund) {
        log.debug("Processing refund for transaction: {}", refund.getTransactionId());
        
        refund.setStatus(RefundRequest.RefundStatus.PROCESSING);
        refund.setProcessedAt(LocalDateTime.now());
        refundRequestRepository.save(refund);
        
        Transaction transaction = transactionRepository.findById(refund.getTransactionId())
                .orElseThrow(() -> new RefundException("Transaction not found"));
        
        try {
            // Step 1: Reverse credit transfer if needed
            if (transaction.getCreditsTransferredAt() != null) {
                refund.setStatus(RefundRequest.RefundStatus.CREDITS_REVERSING);
                refundRequestRepository.save(refund);
                
                reverseCreditTransfer(transaction, refund);
            }
            
            // Step 2: Process payment refund
            refund.setStatus(RefundRequest.RefundStatus.PAYMENT_REFUNDING);
            refundRequestRepository.save(refund);
            
            processPaymentRefund(transaction, refund);
            
            // Step 3: Update escrow if still held
            if (transaction.getEscrowStatus() == Transaction.EscrowStatus.HELD) {
                refundEscrow(transaction, refund);
            }
            
            // Step 4: Update transaction status
            updateTransactionForRefund(transaction, refund);
            
            // Step 5: Mark refund as completed
            refund.setStatus(RefundRequest.RefundStatus.COMPLETED);
            refund.setCompletedAt(LocalDateTime.now());
            refundRequestRepository.save(refund);
            
            log.info("Refund completed successfully: {}", refund.getRefundNumber());
            
            return new RefundProcessingResult(
                refund.getRefundId(),
                refund.getRefundNumber(),
                true,
                "Refund processed successfully"
            );
            
        } catch (Exception e) {
            log.error("Error processing refund: {}", refund.getRefundNumber(), e);
            throw new RefundException("Refund processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Reverse credit transfer
     */
    private void reverseCreditTransfer(Transaction transaction, RefundRequest refund) {
        log.debug("Reversing credit transfer for refund: {}", refund.getRefundNumber());
        
        RefundCreditRequest request = RefundCreditRequest.builder()
                .transactionId(transaction.getTransactionId())
                .fromUserId(transaction.getBuyerId())
                .toUserId(transaction.getSellerId())
                .amount(transaction.getCreditAmountTons())
                .refundId(refund.getRefundId())
                .reason(refund.getReason())
                .build();
        
        UUID reversalId = carbonCreditServiceClient.refundCredits(request);
        
        refund.setCreditsReversed(true);
        refund.setCreditReversalId(reversalId);
        refund.setCreditsReversedAt(LocalDateTime.now());
        refundRequestRepository.save(refund);
    }

    /**
     * Process payment refund
     */
    private void processPaymentRefund(Transaction transaction, RefundRequest refund) {
        log.debug("Processing payment refund for: {}", refund.getRefundNumber());
        
        RefundPaymentRequest request = RefundPaymentRequest.builder()
                .originalPaymentId(transaction.getPaymentId())
                .refundAmount(refund.getRefundAmountVnd())
                .currency("VND")
                .refundId(refund.getRefundId())
                .reason(refund.getReason())
                .build();
        
        String gatewayRefundId = paymentServiceClient.refundPayment(request);
        
        refund.setGatewayRefundId(gatewayRefundId);
        refund.setProcessingReference("PAY-" + gatewayRefundId);
        refundRequestRepository.save(refund);
    }

    /**
     * Refund escrow account
     */
    private void refundEscrow(Transaction transaction, RefundRequest refund) {
        if (transaction.getEscrowAccount() != null) {
            escrowService.refundEscrow(
                transaction.getEscrowAccount().getEscrowAccountId(),
                refund.getReason(),
                refund.getRefundAmountVnd()
            );
        }
    }

    /**
     * Update transaction for refund
     */
    private void updateTransactionForRefund(Transaction transaction, RefundRequest refund) {
        if (refund.getRefundType() == RefundRequest.RefundType.FULL) {
            transaction.setStatus(Transaction.TransactionStatus.REFUNDED);
            transaction.setPaymentStatus(Transaction.PaymentStatus.REFUNDED);
        } else {
            transaction.setPaymentStatus(Transaction.PaymentStatus.PARTIALLY_REFUNDED);
        }
        
        transaction.setRefundAmountVnd(refund.getRefundAmountVnd());
        transaction.setRefundReason(refund.getReason());
        transaction.setRefundApprovedBy(refund.getApprovedBy());
        transaction.setRefundApprovedAt(refund.getApprovedAt());
        
        transactionRepository.save(transaction);
    }

    /**
     * Determine refund type based on amounts
     */
    private RefundRequest.RefundType determineRefundType(BigDecimal refundAmount, 
                                                         BigDecimal originalAmount) {
        if (refundAmount.compareTo(originalAmount) == 0) {
            return RefundRequest.RefundType.FULL;
        } else {
            return RefundRequest.RefundType.PARTIAL;
        }
    }

    /**
     * Get pending approval refunds
     */
    public Page<RefundRequest> getPendingApprovalRefunds(int page, int size) {
        return refundRequestRepository.findByStatusOrderByCreatedAtDesc(
            RefundRequest.RefundStatus.AWAITING_APPROVAL,
            PageRequest.of(page, size)
        );
    }

    /**
     * Get refunds by requester
     */
    public Page<RefundRequest> getRefundsByRequester(UUID requesterId, int page, int size) {
        return refundRequestRepository.findByRequesterIdOrderByCreatedAtDesc(
            requesterId,
            PageRequest.of(page, size)
        );
    }

    /**
     * Cancel expired refund requests (scheduled task)
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour
    public void cancelExpiredRefunds() {
        log.debug("Checking for expired refund requests");
        
        List<RefundRequest> expiredRefunds = refundRequestRepository
                .findExpiredRequests(LocalDateTime.now());
        
        for (RefundRequest refund : expiredRefunds) {
            try {
                refund.setStatus(RefundRequest.RefundStatus.EXPIRED);
                refund.setCancelledAt(LocalDateTime.now());
                refundRequestRepository.save(refund);
                
                log.info("Expired refund request: {}", refund.getRefundNumber());
            } catch (Exception e) {
                log.error("Failed to expire refund: {}", refund.getRefundNumber(), e);
            }
        }
    }

    /**
     * Process auto-approvable refunds (scheduled task)
     */
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    public void processAutoApprovableRefunds() {
        log.debug("Processing auto-approvable refunds");
        
        List<RefundRequest> autoApprovableRefunds = refundRequestRepository
                .findAutoApprovableRequests(autoApproveThreshold);
        
        for (RefundRequest refund : autoApprovableRefunds) {
            try {
                refund.setStatus(RefundRequest.RefundStatus.APPROVED);
                refund.setAutoApproved(true);
                refund.setApprovedAt(LocalDateTime.now());
                refund.setApprovalNotes("Auto-approved: Amount below threshold");
                refundRequestRepository.save(refund);
                
                processRefundAsync(refund);
                
                log.info("Auto-approved refund: {}", refund.getRefundNumber());
            } catch (Exception e) {
                log.error("Failed to auto-approve refund: {}", refund.getRefundNumber(), e);
            }
        }
    }

    /**
     * Retry failed refunds (scheduled task)
     */
    @Scheduled(cron = "0 */30 * * * ?") // Every 30 minutes
    public void retryFailedRefunds() {
        log.debug("Retrying failed refunds");
        
        LocalDateTime retryAfter = LocalDateTime.now().minusHours(1);
        List<RefundRequest> failedRefunds = refundRequestRepository
                .findRequestsForRetry(retryAfter);
        
        for (RefundRequest refund : failedRefunds) {
            try {
                log.info("Retrying refund: {}", refund.getRefundNumber());
                processRefundAsync(refund);
            } catch (Exception e) {
                log.error("Retry failed for refund: {}", refund.getRefundNumber(), e);
            }
        }
    }

    /**
     * Get refund statistics
     */
    public RefundStatistics getRefundStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> stats = refundRequestRepository.getRefundStatistics(startDate, endDate);
        List<Object[]> reasons = refundRequestRepository.getRefundReasonDistribution(startDate, endDate);
        
        Long totalRefunds = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        if (!stats.isEmpty() && stats.get(0) != null) {
            Object[] row = stats.get(0);
            totalRefunds = (Long) row[0];
            totalAmount = (BigDecimal) row[1];
        }
        
        return RefundStatistics.builder()
                .totalRefunds(totalRefunds)
                .totalRefundedAmount(totalAmount)
                .reasonDistribution(reasons)
                .build();
    }
    
    // Inner classes
    @lombok.Data
    @lombok.Builder
    public static class RefundRequestDto {
        private UUID transactionId;
        private BigDecimal refundAmount;
        private UUID requesterId;
        private String requesterType;
        private String reason;
        private RefundRequest.RefundReasonCategory reasonCategory;
        private String supportingDocuments;
        private String idempotencyKey;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RefundProcessingResult {
        private UUID refundId;
        private String refundNumber;
        private boolean success;
        private String message;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RefundStatistics {
        private Long totalRefunds;
        private BigDecimal totalRefundedAmount;
        private List<Object[]> reasonDistribution;
    }
}
