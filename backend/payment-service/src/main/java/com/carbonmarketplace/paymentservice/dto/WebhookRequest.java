package com.carbonmarketplace.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {

    // Common fields across payment gateways
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("reference_id")
    private String referenceId;

    private BigDecimal amount;

    private String currency;

    private String status;

    @JsonProperty("result_code")
    private String resultCode;

    private String message;

    private String signature;

    // MoMo specific fields
    @JsonProperty("partnerCode")
    private String partnerCode;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("accessKey")
    private String accessKey;

    @JsonProperty("orderInfo")
    private String orderInfo;

    @JsonProperty("orderType")
    private String orderType;

    @JsonProperty("transId")
    private Long transId;

    @JsonProperty("payType")
    private String payType;

    @JsonProperty("responseTime")
    private Long responseTime;

    // VNPay specific fields
    @JsonProperty("vnp_TmnCode")
    private String vnpTmnCode;

    @JsonProperty("vnp_TxnRef")
    private String vnpTxnRef;

    @JsonProperty("vnp_Amount")
    private String vnpAmount;

    @JsonProperty("vnp_OrderInfo")
    private String vnpOrderInfo;

    @JsonProperty("vnp_ResponseCode")
    private String vnpResponseCode;

    @JsonProperty("vnp_TransactionNo")
    private String vnpTransactionNo;

    @JsonProperty("vnp_BankCode")
    private String vnpBankCode;

    @JsonProperty("vnp_PayDate")
    private String vnpPayDate;

    @JsonProperty("vnp_SecureHash")
    private String vnpSecureHash;

    @JsonProperty("vnp_TransactionStatus")
    private String vnpTransactionStatus;

    // Capture any additional fields
    @Builder.Default
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        if (additionalProperties == null) {
            additionalProperties = new HashMap<>();
        }
        additionalProperties.put(key, value);
    }
}
