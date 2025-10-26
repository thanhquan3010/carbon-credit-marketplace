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
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_payment_reference", columnList = "external_reference")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "virtual_account_number", length = 50)
    private String virtualAccountNumber;

    @Column(name = "virtual_account_name", length = 100)
    private String virtualAccountName;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "platform_fee", precision = 19, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "gateway_fee", precision = 19, scale = 2)
    private BigDecimal gatewayFee;

    @Column(name = "net_amount", precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum PaymentMethod {
        MOMO,
        VNPAY,
        ZALOPAY,
        SHOPEEPAY,
        BANK_TRANSFER,
        STRIPE,
        INVOICE
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        EXPIRED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
}
