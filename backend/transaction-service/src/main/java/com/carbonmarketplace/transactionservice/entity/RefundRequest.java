package com.carbonmarketplace.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refund_requests", indexes = {
        @Index(name = "idx_refund_transaction", columnList = "transaction_id"),
        @Index(name = "idx_refund_status", columnList = "status"),
        @Index(name = "idx_refund_requester", columnList = "requester_id"),
        @Index(name = "idx_refund_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "refundId")
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refund_id")
    private UUID refundId;

    @Column(name = "refund_number", unique = true, nullable = false, length = 50)
    private String refundNumber;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "original_payment_id")
    private UUID originalPaymentId;

    // Refund Details
    @Column(name = "refund_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmountVnd;

    @Column(name = "original_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmountVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false, length = 30)
    private RefundType refundType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    // Request Information
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "requester_type", nullable = false, length = 30)
    private String requesterType; // BUYER, ADMIN, SYSTEM

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", length = 50)
    private RefundReasonCategory reasonCategory;

    @Column(name = "supporting_documents", columnDefinition = "TEXT")
    private String supportingDocuments;

    // Approval Workflow
    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = true;

    @Column(name = "auto_approved")
    @Builder.Default
    private Boolean autoApproved = false;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Processing Information
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processing_reference", length = 100)
    private String processingReference;

    @Column(name = "gateway_refund_id", length = 100)
    private String gatewayRefundId;

    @Column(name = "refund_method", length = 50)
    private String refundMethod; // ORIGINAL_PAYMENT_METHOD, BANK_TRANSFER, CREDIT

    // Credit Reversal
    @Column(name = "credits_reversed")
    @Builder.Default
    private Boolean creditsReversed = false;

    @Column(name = "credit_reversal_id")
    private UUID creditReversalId;

    @Column(name = "credits_reversed_at")
    private LocalDateTime creditsReversedAt;

    // Financial Impact
    @Column(name = "platform_fee_refund_vnd", precision = 15, scale = 2)
    private BigDecimal platformFeeRefundVnd;

    @Column(name = "seller_debit_vnd", precision = 15, scale = 2)
    private BigDecimal sellerDebitVnd;

    @Column(name = "net_refund_vnd", precision = 15, scale = 2)
    private BigDecimal netRefundVnd;

    // Timeline
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Error Tracking
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    // Metadata
    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum RefundStatus {
        PENDING, // Initial request
        AWAITING_APPROVAL, // Waiting for manual approval
        APPROVED, // Approved for processing
        REJECTED, // Approval rejected
        PROCESSING, // Being processed
        CREDITS_REVERSING, // Reversing credit transfer
        PAYMENT_REFUNDING, // Refunding payment
        COMPLETED, // Successfully refunded
        FAILED, // Refund failed
        CANCELLED, // Request cancelled
        EXPIRED // Request expired
    }

    public enum RefundType {
        FULL, // Full refund
        PARTIAL, // Partial refund
        CREDIT_ADJUSTMENT, // Credit only adjustment
        GOODWILL, // Goodwill refund
        DISPUTE // Dispute resolution
    }

    public enum RefundReasonCategory {
        DUPLICATE_PAYMENT,
        WRONG_AMOUNT,
        SERVICE_NOT_PROVIDED,
        QUALITY_ISSUE,
        TECHNICAL_ERROR,
        FRAUD,
        CUSTOMER_REQUEST,
        ADMIN_OVERRIDE,
        OTHER,
        DISPUTE
    }

    // Business Methods
    public void generateRefundNumber() {
        this.refundNumber = "RFD-" + System.currentTimeMillis() +
                "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public boolean requiresManualApproval(BigDecimal thresholdVnd) {
        return refundAmountVnd.compareTo(thresholdVnd) > 0 ||
                refundType == RefundType.DISPUTE ||
                reasonCategory == RefundReasonCategory.FRAUD;
    }

    public boolean canAutoApprove(BigDecimal autoApproveThresholdVnd) {
        return refundAmountVnd.compareTo(autoApproveThresholdVnd) <= 0 &&
                refundType == RefundType.FULL &&
                reasonCategory != RefundReasonCategory.FRAUD &&
                reasonCategory != RefundReasonCategory.DISPUTE;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean canRetry() {
        return status == RefundStatus.FAILED &&
                retryCount < maxRetries &&
                !isExpired();
    }

    public void calculateFinancialImpact(BigDecimal platformFeePercentage) {
        if (refundType == RefundType.FULL) {
            this.platformFeeRefundVnd = originalAmountVnd.multiply(platformFeePercentage)
                    .divide(new BigDecimal(100));
            this.sellerDebitVnd = refundAmountVnd.subtract(platformFeeRefundVnd);
            this.netRefundVnd = refundAmountVnd;
        } else if (refundType == RefundType.PARTIAL) {
            BigDecimal refundRatio = refundAmountVnd.divide(originalAmountVnd, 4, BigDecimal.ROUND_HALF_UP);
            this.platformFeeRefundVnd = originalAmountVnd.multiply(platformFeePercentage)
                    .divide(new BigDecimal(100))
                    .multiply(refundRatio);
            this.sellerDebitVnd = refundAmountVnd.subtract(platformFeeRefundVnd);
            this.netRefundVnd = refundAmountVnd;
        }
    }
}
