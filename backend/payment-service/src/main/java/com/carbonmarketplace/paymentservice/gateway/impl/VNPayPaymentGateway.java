package com.carbonmarketplace.paymentservice.gateway.impl;

import com.carbonmarketplace.paymentservice.dto.PaymentRequest;
import com.carbonmarketplace.paymentservice.dto.PaymentResponse;
import com.carbonmarketplace.paymentservice.dto.RefundRequest;
import com.carbonmarketplace.paymentservice.dto.WebhookRequest;
import com.carbonmarketplace.paymentservice.entity.Payment;
import com.carbonmarketplace.paymentservice.gateway.PaymentGateway;
import com.carbonmarketplace.paymentservice.gateway.PaymentGatewayException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayPaymentGateway implements PaymentGateway {

    private static final String VNPAY_API_URL = "https://pay.vnpay.vn/vpcpay.html";
    private static final String VNPAY_SANDBOX_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String VNPAY_VERSION = "2.1.0";
    private static final String VNPAY_COMMAND_PAY = "pay";
    private static final String VNPAY_COMMAND_REFUND = "refund";
    private static final String HMAC_SHA512 = "HmacSHA512";

    @Value("${payment.vnpay.tmn-code}")
    private String tmnCode;

    @Value("${payment.vnpay.hash-secret}")
    private String hashSecret;

    @Value("${payment.vnpay.environment:sandbox}")
    private String environment;

    @Value("${payment.vnpay.refund-url}")
    private String refundUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException {
        try {
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", VNPAY_VERSION);
            vnpParams.put("vnp_Command", VNPAY_COMMAND_PAY);
            vnpParams.put("vnp_TmnCode", tmnCode);
            vnpParams.put("vnp_Amount", String.valueOf(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", "CARBON-" + request.getTransactionId());
            vnpParams.put("vnp_OrderInfo", request.getOrderInfo() != null ? request.getOrderInfo() : "Carbon Credit Purchase");
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", request.getRedirectUrl());
            vnpParams.put("vnp_IpAddr", "127.0.0.1"); // Should get from request
            vnpParams.put("vnp_CreateDate", getCurrentTimeString());
            vnpParams.put("vnp_ExpireDate", getExpireTimeString());
            
            if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
                vnpParams.put("vnp_BankCode", request.getBankCode());
            }
            
            // Build query string
            String queryString = buildQueryString(vnpParams);
            String hashData = buildHashData(vnpParams);
            String vnpSecureHash = generateHmacSHA512(hashData, hashSecret);
            
            String paymentUrl = getApiUrl() + "?" + queryString + "&vnp_SecureHash=" + vnpSecureHash;
            
            return PaymentResponse.builder()
                .paymentId(UUID.randomUUID())
                .transactionId(request.getTransactionId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .paymentUrl(paymentUrl)
                .externalReference(vnpParams.get("vnp_TxnRef"))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .message("Payment created successfully")
                .build();
                
        } catch (Exception e) {
            log.error("Error creating VNPay payment", e);
            throw new PaymentGatewayException("Failed to create VNPay payment", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(WebhookRequest request, String signature) {
        try {
            // VNPay IPN verification
            Map<String, String> vnpParams = new TreeMap<>();
            
            // Add all VNPay parameters from webhook
            if (request.getVnpTmnCode() != null) vnpParams.put("vnp_TmnCode", request.getVnpTmnCode());
            if (request.getVnpAmount() != null) vnpParams.put("vnp_Amount", request.getVnpAmount());
            if (request.getVnpBankCode() != null) vnpParams.put("vnp_BankCode", request.getVnpBankCode());
            if (request.getVnpOrderInfo() != null) vnpParams.put("vnp_OrderInfo", request.getVnpOrderInfo());
            if (request.getVnpPayDate() != null) vnpParams.put("vnp_PayDate", request.getVnpPayDate());
            if (request.getVnpResponseCode() != null) vnpParams.put("vnp_ResponseCode", request.getVnpResponseCode());
            if (request.getVnpTransactionNo() != null) vnpParams.put("vnp_TransactionNo", request.getVnpTransactionNo());
            if (request.getVnpTransactionStatus() != null) vnpParams.put("vnp_TransactionStatus", request.getVnpTransactionStatus());
            if (request.getVnpTxnRef() != null) vnpParams.put("vnp_TxnRef", request.getVnpTxnRef());
            
            // Add additional properties
            for (Map.Entry<String, Object> entry : request.getAdditionalProperties().entrySet()) {
                if (entry.getKey().startsWith("vnp_") && !entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType")) {
                    vnpParams.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            String hashData = buildHashData(vnpParams);
            String expectedSignature = generateHmacSHA512(hashData, hashSecret);
            
            return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
            
        } catch (Exception e) {
            log.error("Error verifying VNPay webhook signature", e);
            return false;
        }
    }

    @Override
    public Payment processWebhook(WebhookRequest request) throws PaymentGatewayException {
        try {
            // Parse VNPay IPN response
            String responseCode = request.getVnpResponseCode();
            String transactionStatus = request.getVnpTransactionStatus();
            
            Payment.PaymentStatus status;
            
            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                status = Payment.PaymentStatus.COMPLETED;
            } else if ("24".equals(responseCode)) {
                status = Payment.PaymentStatus.CANCELLED;
            } else if ("15".equals(responseCode)) {
                status = Payment.PaymentStatus.EXPIRED;
            } else {
                status = Payment.PaymentStatus.FAILED;
            }
            
            Payment payment = new Payment();
            payment.setStatus(status);
            payment.setGatewayTransactionId(request.getVnpTransactionNo());
            payment.setExternalReference(request.getVnpTxnRef());
            payment.setGatewayResponse(objectMapper.writeValueAsString(request));
            
            if (status == Payment.PaymentStatus.COMPLETED) {
                payment.setPaidAt(parseVNPayDate(request.getVnpPayDate()));
                payment.setBankCode(request.getVnpBankCode());
            } else if (status == Payment.PaymentStatus.FAILED) {
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason(getVNPayResponseMessage(responseCode));
            }
            
            return payment;
            
        } catch (Exception e) {
            log.error("Error processing VNPay webhook", e);
            throw new PaymentGatewayException("Failed to process VNPay webhook", e);
        }
    }

    @Override
    public Payment queryPaymentStatus(String transactionId) throws PaymentGatewayException {
        try {
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", VNPAY_VERSION);
            vnpParams.put("vnp_Command", "querydr");
            vnpParams.put("vnp_TmnCode", tmnCode);
            vnpParams.put("vnp_TxnRef", "CARBON-" + transactionId);
            vnpParams.put("vnp_OrderInfo", "Query transaction status");
            vnpParams.put("vnp_CreateDate", getCurrentTimeString());
            vnpParams.put("vnp_IpAddr", "127.0.0.1");
            
            String hashData = buildHashData(vnpParams);
            String vnpSecureHash = generateHmacSHA512(hashData, hashSecret);
            vnpParams.put("vnp_SecureHash", vnpSecureHash);
            
            // Call VNPay Query API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String requestBody = vnpParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            String queryUrl = "production".equalsIgnoreCase(environment) 
                ? "https://api.vnpay.vn/merchant_webapi/api/transaction"
                : "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
                
            ResponseEntity<Map> response = restTemplate.postForEntity(queryUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                Payment payment = new Payment();
                String responseCode = String.valueOf(responseBody.get("vnp_ResponseCode"));
                
                if ("00".equals(responseCode)) {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaidAt(LocalDateTime.now());
                } else {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailedAt(LocalDateTime.now());
                    payment.setFailureReason(getVNPayResponseMessage(responseCode));
                }
                
                payment.setGatewayResponse(objectMapper.writeValueAsString(responseBody));
                return payment;
            }
            
            throw new PaymentGatewayException("Invalid response from VNPay query API");
            
        } catch (Exception e) {
            log.error("Error querying VNPay payment status", e);
            throw new PaymentGatewayException("Failed to query payment status", e);
        }
    }

    @Override
    public Payment processRefund(RefundRequest request) throws PaymentGatewayException {
        try {
            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", VNPAY_VERSION);
            vnpParams.put("vnp_Command", VNPAY_COMMAND_REFUND);
            vnpParams.put("vnp_TmnCode", tmnCode);
            vnpParams.put("vnp_TxnRef", "CARBON-" + request.getTransactionId());
            vnpParams.put("vnp_Amount", String.valueOf(request.getRefundAmount().multiply(BigDecimal.valueOf(100)).longValue()));
            vnpParams.put("vnp_OrderInfo", "Refund: " + request.getRefundReason());
            vnpParams.put("vnp_TransactionNo", request.getPaymentId().toString());
            vnpParams.put("vnp_TransactionDate", getCurrentTimeString());
            vnpParams.put("vnp_CreateBy", request.getRequestedBy() != null ? request.getRequestedBy().toString() : "system");
            vnpParams.put("vnp_CreateDate", getCurrentTimeString());
            vnpParams.put("vnp_IpAddr", "127.0.0.1");
            
            String hashData = buildHashData(vnpParams);
            String vnpSecureHash = generateHmacSHA512(hashData, hashSecret);
            vnpParams.put("vnp_SecureHash", vnpSecureHash);
            
            // Call VNPay Refund API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String requestBody = vnpParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(refundUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                Payment payment = new Payment();
                String responseCode = String.valueOf(responseBody.get("vnp_ResponseCode"));
                
                if ("00".equals(responseCode)) {
                    if (request.getRefundType() == RefundRequest.RefundType.FULL) {
                        payment.setStatus(Payment.PaymentStatus.REFUNDED);
                    } else {
                        payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
                    }
                } else {
                    throw new PaymentGatewayException(
                        "VNPay refund failed",
                        responseCode,
                        getVNPayResponseMessage(responseCode)
                    );
                }
                
                payment.setGatewayResponse(objectMapper.writeValueAsString(responseBody));
                return payment;
            }
            
            throw new PaymentGatewayException("Invalid response from VNPay refund API");
            
        } catch (Exception e) {
            log.error("Error processing VNPay refund", e);
            throw new PaymentGatewayException("Failed to process refund", e);
        }
    }

    @Override
    public boolean supports(Payment.PaymentMethod paymentMethod) {
        return Payment.PaymentMethod.VNPAY == paymentMethod;
    }

    @Override
    public String getGatewayName() {
        return "VNPay";
    }

    private String getApiUrl() {
        return "production".equalsIgnoreCase(environment) ? VNPAY_API_URL : VNPAY_SANDBOX_URL;
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
            .map(e -> {
                try {
                    return e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8.toString());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            })
            .collect(Collectors.joining("&"));
    }

    private String buildHashData(Map<String, String> params) {
        return params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
    }

    private String generateHmacSHA512(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC SHA512", e);
        }
    }

    private String getCurrentTimeString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(new Date());
    }

    private String getExpireTimeString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 15);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(calendar.getTime());
    }

    private LocalDateTime parseVNPayDate(String vnpDate) {
        if (vnpDate == null || vnpDate.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(vnpDate, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse VNPay date: {}", vnpDate, e);
            return LocalDateTime.now();
        }
    }

    private String getVNPayResponseMessage(String code) {
        Map<String, String> messages = new HashMap<>();
        messages.put("00", "Transaction successful");
        messages.put("07", "Successful deduction. Suspected fraudulent transaction");
        messages.put("09", "Transaction unsuccessful due to: Customer's card/account not registered for InternetBanking service");
        messages.put("10", "Transaction unsuccessful due to: Customer verification incorrect over 3 times");
        messages.put("11", "Transaction unsuccessful due to: Expired payment deadline");
        messages.put("12", "Transaction unsuccessful due to: Card/Account locked");
        messages.put("13", "Transaction unsuccessful due to: Incorrect transaction password");
        messages.put("24", "Transaction unsuccessful due to: Customer cancelled transaction");
        messages.put("51", "Transaction unsuccessful due to: Insufficient account balance");
        messages.put("65", "Transaction unsuccessful due to: Exceeds daily transaction limit");
        messages.put("75", "Payment bank is under maintenance");
        messages.put("79", "Transaction unsuccessful due to: Customer entered payment password incorrectly over the specified number of times");
        messages.put("99", "Other errors");
        
        return messages.getOrDefault(code, "Unknown error code: " + code);
    }
}
