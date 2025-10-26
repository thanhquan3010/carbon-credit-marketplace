package com.carbonmarketplace.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payouts", indexes = {
    @Index(name = "idx_payout_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payout_seller_id", columnList = "seller_id"),
    @Index(name = "idx_payout_status", columnList = "status"),
    @Index(name = "idx_payout_settlement_date", columnList = "settlement_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payout_id")
    private UUID payoutId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "gateway_fee", precision = 19, scale = 2)
    private BigDecimal gatewayFee;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayoutStatus status;

    @Column(name = "bank_account_id")
    private UUID bankAccountId;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum PayoutStatus {
        PENDING,
        SCHEDULED,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        ON_HOLD
    }
}
