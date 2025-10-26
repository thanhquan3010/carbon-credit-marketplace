package com.carbonmarketplace.paymentservice.dto;

import com.carbonmarketplace.paymentservice.entity.Payment;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Transaction ID is required")
    @JsonProperty("transaction_id")
    private UUID transactionId;

    @NotNull(message = "User ID is required")
    @JsonProperty("user_id")
    private UUID userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100000", message = "Minimum payment amount is 100,000 VND")
    @DecimalMax(value = "10000000000", message = "Maximum payment amount is 10,000,000,000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "VND|USD", message = "Currency must be VND or USD")
    private String currency = "VND";

    @NotNull(message = "Payment method is required")
    @JsonProperty("payment_method")
    private Payment.PaymentMethod paymentMethod;

    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @NotBlank(message = "Redirect URL is required")
    @Pattern(regexp = "^https?://.*", message = "Redirect URL must be a valid HTTP/HTTPS URL")
    @JsonProperty("redirect_url")
    private String redirectUrl;

    @NotBlank(message = "Callback URL is required")
    @Pattern(regexp = "^https?://.*", message = "Callback URL must be a valid HTTP/HTTPS URL")
    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("order_info")
    private String orderInfo;

    @JsonProperty("customer_info")
    private CustomerInfo customerInfo;

    @JsonProperty("billing_info")
    private BillingInfo billingInfo;

    @JsonProperty("bank_code")
    private String bankCode;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        @NotBlank(message = "Customer name is required")
        private String name;
        
        @Email(message = "Invalid email format")
        private String email;
        
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
        private String phone;
        
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingInfo {
        @JsonProperty("company_name")
        private String companyName;
        
        @JsonProperty("tax_code")
        @Pattern(regexp = "^[0-9]{10,13}$", message = "Tax code must be 10-13 digits")
        private String taxCode;
        
        private String address;
        
        private String city;
        
        @JsonProperty("postal_code")
        private String postalCode;
        
        private String country = "VN";
    }
}
