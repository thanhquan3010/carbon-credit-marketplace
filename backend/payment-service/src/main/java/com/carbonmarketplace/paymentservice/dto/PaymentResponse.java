package com.carbonmarketplace.paymentservice.dto;

import com.carbonmarketplace.paymentservice.entity.Payment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    @JsonProperty("payment_id")
    private UUID paymentId;

    @JsonProperty("transaction_id")
    private UUID transactionId;

    private BigDecimal amount;

    private String currency;

    @JsonProperty("payment_method")
    private Payment.PaymentMethod paymentMethod;

    private Payment.PaymentStatus status;

    @JsonProperty("payment_url")
    private String paymentUrl;

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("gateway_transaction_id")
    private String gatewayTransactionId;

    @JsonProperty("virtual_account")
    private VirtualAccountInfo virtualAccount;

    @JsonProperty("qr_code")
    private QRCodeInfo qrCode;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private String message;

    @JsonProperty("additional_data")
    private Map<String, Object> additionalData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VirtualAccountInfo {
        @JsonProperty("account_number")
        private String accountNumber;
        
        @JsonProperty("account_name")
        private String accountName;
        
        @JsonProperty("bank_code")
        private String bankCode;
        
        @JsonProperty("bank_name")
        private String bankName;
        
        @JsonProperty("reference_code")
        private String referenceCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QRCodeInfo {
        @JsonProperty("qr_data")
        private String qrData;
        
        @JsonProperty("qr_image_url")
        private String qrImageUrl;
        
        @JsonProperty("qr_type")
        private String qrType;
    }
}
