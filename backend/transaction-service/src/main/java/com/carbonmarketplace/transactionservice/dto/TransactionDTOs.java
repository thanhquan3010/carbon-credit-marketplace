package com.carbonmarketplace.transactionservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Objects for Transaction Service
 */
public class TransactionDTOs {

    // Payment Service DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessPaymentRequest {
        private UUID transactionId;
        private BigDecimal amount;
        private String currency;
        private UUID buyerId;
        private String description;
        private String paymentMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResult {
        private UUID paymentId;
        private String status;
        private String gatewayTransactionId;
        private String paymentUrl;
        private LocalDateTime processedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentStatus {
        private UUID paymentId;
        private String status;
        private BigDecimal amount;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayoutRequest {
        private UUID transactionId;
        private UUID paymentId;
        private UUID sellerId;
        private BigDecimal amount;
        private String currency;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayoutResult {
        private UUID payoutId;
        private String status;
        private String referenceNumber;
        private LocalDateTime scheduledDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundPaymentRequest {
        private UUID originalPaymentId;
        private BigDecimal refundAmount;
        private String currency;
        private UUID refundId;
        private String reason;
    }

    // Carbon Credit Service DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LockCreditsRequest {
        private UUID listingId;
        private BigDecimal amount;
        private UUID transactionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditLockResult {
        private String lockId;
        private BigDecimal lockedAmount;
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferCreditsRequest {
        private UUID fromUserId;
        private UUID toUserId;
        private BigDecimal amount;
        private String lockId;
        private UUID transactionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditTransferResult {
        private UUID transferId;
        private BigDecimal transferredAmount;
        private String newBalance;
        private LocalDateTime transferredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundCreditRequest {
        private UUID transactionId;
        private UUID fromUserId;
        private UUID toUserId;
        private BigDecimal amount;
        private UUID refundId;
        private String reason;
    }

    // Certificate Service DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateCertificateRequest {
        private UUID transactionId;
        private UUID buyerId;
        private BigDecimal creditAmount;
        private UUID transferId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateResult {
        private UUID certificateId;
        private String certificateNumber;
        private String certificateUrl;
        private LocalDateTime issuedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateDetails {
        private UUID certificateId;
        private String certificateNumber;
        private String status;
        private UUID buyerId;
        private BigDecimal creditAmount;
        private LocalDateTime issuedAt;
    }

    // Notification Service DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionNotificationRequest {
        private UUID transactionId;
        private UUID buyerId;
        private UUID sellerId;
        private String certificateUrl;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementNotificationRequest {
        private UUID batchId;
        private String batchNumber;
        private Integer transactionCount;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundNotificationRequest {
        private UUID refundId;
        private UUID transactionId;
        private UUID userId;
        private BigDecimal refundAmount;
        private String reason;
    }

    // Settlement DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementScheduleResult {
        private UUID settlementId;
        private LocalDateTime scheduledDate;
        private String batchNumber;
    }
}
