package com.carbonmarketplace.carboncreditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing credit transactions and history
 */
@Entity
@Table(name = "credit_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id", nullable = false)
    @ToString.Exclude
    private CarbonCredit carbonCredit;
    
    // Transaction Type
    @Column(name = "transaction_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    // Parties involved
    @Column(name = "from_user_id")
    private UUID fromUserId;
    
    @Column(name = "to_user_id")
    private UUID toUserId;
    
    // Transaction Details
    @Column(name = "amount_tons", nullable = false, precision = 10, scale = 4)
    private BigDecimal amountTons;
    
    @Column(name = "unit_price_vnd", precision = 15, scale = 2)
    private BigDecimal unitPriceVnd;
    
    @Column(name = "total_value_vnd", precision = 15, scale = 2)
    private BigDecimal totalValueVnd;
    
    @Column(name = "platform_fee_vnd", precision = 15, scale = 2)
    private BigDecimal platformFeeVnd;
    
    // Reference to marketplace transaction if applicable
    @Column(name = "marketplace_transaction_id")
    private UUID marketplaceTransactionId;
    
    // Transaction Status
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;
    
    // Additional Information
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
    
    public enum TransactionType {
        ISSUANCE,           // Initial issuance from verification
        TRANSFER,           // Transfer between users
        SALE,               // Sale through marketplace
        PURCHASE,           // Purchase through marketplace
        RETIREMENT,         // Retirement of credits
        CANCELLATION,       // Credit cancellation
        ADJUSTMENT          // Administrative adjustment
    }
    
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REVERSED
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
    }
    
    /**
     * Complete the transaction
     */
    public void complete() {
        if (status != TransactionStatus.PENDING && status != TransactionStatus.PROCESSING) {
            throw new IllegalStateException("Cannot complete transaction in status: " + status);
        }
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Cancel the transaction
     */
    public void cancel(String reason) {
        if (status == TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed transaction");
        }
        this.status = TransactionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }
    
    /**
     * Calculate the net amount after fees
     */
    public BigDecimal getNetAmountVnd() {
        if (totalValueVnd != null && platformFeeVnd != null) {
            return totalValueVnd.subtract(platformFeeVnd);
        }
        return totalValueVnd;
    }
}
