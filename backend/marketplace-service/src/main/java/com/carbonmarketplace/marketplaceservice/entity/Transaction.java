package com.carbonmarketplace.marketplaceservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a marketplace transaction.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_buyer", columnList = "buyer_id"),
    @Index(name = "idx_transaction_seller", columnList = "seller_id"),
    @Index(name = "idx_transaction_listing", columnList = "listing_id"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "transaction_code", unique = true, nullable = false)
    private String transactionCode;
    
    @Column(name = "listing_id", nullable = false)
    private UUID listingId;
    
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;
    
    @Column(name = "buyer_name")
    private String buyerName;
    
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;
    
    @Column(name = "seller_name")
    private String sellerName;
    
    @Column(name = "credit_amount_tons", nullable = false, precision = 10, scale = 3)
    private BigDecimal creditAmountTons;
    
    @Column(name = "price_per_ton_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal pricePerTonVnd;
    
    @Column(name = "subtotal_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalVnd;
    
    @Column(name = "platform_fee_vnd", precision = 15, scale = 2)
    private BigDecimal platformFeeVnd;
    
    @Column(name = "total_amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountVnd;
    
    @Column(name = "seller_net_amount_vnd", precision = 15, scale = 2)
    private BigDecimal sellerNetAmountVnd;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    
    @Column(name = "payment_reference")
    private String paymentReference;
    
    @Column(name = "payment_completed_at")
    private LocalDateTime paymentCompletedAt;
    
    @Column(name = "credits_transferred_at")
    private LocalDateTime creditsTransferredAt;
    
    @Column(name = "settlement_completed_at")
    private LocalDateTime settlementCompletedAt;
    
    @Column(name = "certificate_id")
    private String certificateId;
    
    @Column(name = "certificate_url")
    private String certificateUrl;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @Column(name = "escrow_account")
    private String escrowAccount;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    public enum TransactionType {
        FIXED_PRICE,
        AUCTION,
        BULK_PURCHASE
    }
    
    public enum TransactionStatus {
        PENDING,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        CREDITS_TRANSFERRING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED,
        DISPUTED
    }
    
    public enum PaymentMethod {
        MOMO,
        VNPAY,
        ZALOPAY,
        BANK_TRANSFER,
        WALLET_BALANCE
    }
}
