package com.carbonmarketplace.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_trans_buyer", columnList = "buyer_id"),
    @Index(name = "idx_trans_seller", columnList = "seller_id"),
    @Index(name = "idx_trans_listing", columnList = "listing_id"),
    @Index(name = "idx_trans_status", columnList = "status"),
    @Index(name = "idx_trans_created", columnList = "created_at DESC"),
    @Index(name = "idx_trans_payment_status", columnList = "payment_status"),
    @Index(name = "idx_trans_processing", columnList = "status, payment_status, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "transactionId")
@ToString(exclude = {"escrowAccount", "sagaSteps"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    // Transaction Details
    @Column(name = "credit_amount_tons", nullable = false, precision = 10, scale = 4)
    private BigDecimal creditAmountTons;

    @Column(name = "unit_price_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceVnd;

    @Column(name = "total_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountVnd;

    @Column(name = "platform_fee_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal platformFeeVnd;

    @Column(name = "seller_receives_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellerReceivesVnd;

    // Status Management
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", length = 50)
    @Builder.Default
    private EscrowStatus escrowStatus = EscrowStatus.PENDING;

    // External References
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "certificate_id")
    private UUID certificateId;

    // Billing Information
    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "buyer_email", length = 200)
    private String buyerEmail;

    @Column(name = "buyer_phone", length = 20)
    private String buyerPhone;

    @Column(name = "buyer_company", length = 200)
    private String buyerCompany;

    @Column(name = "buyer_tax_id", length = 50)
    private String buyerTaxId;

    @Column(name = "buyer_address", columnDefinition = "TEXT")
    private String buyerAddress;

    // Settlement Information
    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "settlement_batch_id")
    private UUID settlementBatchId;

    @Column(name = "is_express_settlement")
    @Builder.Default
    private Boolean isExpressSettlement = false;

    // Refund Information
    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refund_amount_vnd", precision = 15, scale = 2)
    private BigDecimal refundAmountVnd;

    @Column(name = "refund_approved_by")
    private UUID refundApprovedBy;

    @Column(name = "refund_approved_at")
    private LocalDateTime refundApprovedAt;

    // Timestamps
    @Column(name = "payment_initiated_at")
    private LocalDateTime paymentInitiatedAt;

    @Column(name = "payment_completed_at")
    private LocalDateTime paymentCompletedAt;

    @Column(name = "credits_transferred_at")
    private LocalDateTime creditsTransferredAt;

    @Column(name = "certificate_issued_at")
    private LocalDateTime certificateIssuedAt;

    @Column(name = "settlement_completed_at")
    private LocalDateTime settlementCompletedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    // Metadata
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Relationships
    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EscrowAccount escrowAccount;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SagaStep> sagaSteps = new ArrayList<>();

    // Enums
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        AWAITING_PAYMENT,
        PAYMENT_RECEIVED,
        TRANSFERRING_CREDITS,
        GENERATING_CERTIFICATE,
        IN_SETTLEMENT,
        COMPLETED,
        CANCELLED,
        FAILED,
        REFUNDING,
        REFUNDED
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }

    public enum EscrowStatus {
        PENDING,
        HELD,
        RELEASED,
        REFUNDED,
        EXPIRED
    }

    // Business Methods
    public boolean isRefundable() {
        return status == TransactionStatus.COMPLETED && 
               paymentStatus == PaymentStatus.COMPLETED &&
               completedAt != null &&
               completedAt.plusDays(30).isAfter(LocalDateTime.now());
    }

    public boolean isSettleable() {
        return status == TransactionStatus.IN_SETTLEMENT &&
               escrowStatus == EscrowStatus.HELD &&
               settlementDate != null &&
               settlementDate.isBefore(LocalDateTime.now());
    }

    public boolean requiresManualApproval() {
        return totalAmountVnd.compareTo(new BigDecimal("10000000")) > 0; // > 10M VND
    }

    public void addSagaStep(SagaStep step) {
        sagaSteps.add(step);
        step.setTransaction(this);
    }
}
