package com.carbonmarketplace.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_webhooks", indexes = {
    @Index(name = "idx_webhook_payment_id", columnList = "payment_id"),
    @Index(name = "idx_webhook_gateway", columnList = "gateway"),
    @Index(name = "idx_webhook_status", columnList = "processing_status"),
    @Index(name = "idx_webhook_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "webhook_id")
    private UUID webhookId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "gateway", nullable = false, length = 50)
    private String gateway;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT", nullable = false)
    private String requestBody;

    @Column(name = "signature", length = 500)
    private String signature;

    @Column(name = "is_valid_signature")
    private Boolean isValidSignature;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "processing_error", length = 500)
    private String processingError;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum ProcessingStatus {
        RECEIVED,
        PROCESSING,
        PROCESSED,
        FAILED,
        INVALID_SIGNATURE,
        DUPLICATE
    }
}
