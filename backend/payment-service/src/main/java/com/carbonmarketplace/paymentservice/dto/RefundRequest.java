package com.carbonmarketplace.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "Payment ID is required")
    @JsonProperty("payment_id")
    private UUID paymentId;

    @NotNull(message = "Transaction ID is required")
    @JsonProperty("transaction_id")
    private UUID transactionId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be positive")
    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;

    @NotBlank(message = "Refund reason is required")
    @Size(min = 10, max = 500, message = "Refund reason must be between 10 and 500 characters")
    @JsonProperty("refund_reason")
    private String refundReason;

    @JsonProperty("refund_type")
    private RefundType refundType = RefundType.FULL;

    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("requested_by")
    private UUID requestedBy;

    @JsonProperty("notes")
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public enum RefundType {
        FULL,
        PARTIAL
    }
}
