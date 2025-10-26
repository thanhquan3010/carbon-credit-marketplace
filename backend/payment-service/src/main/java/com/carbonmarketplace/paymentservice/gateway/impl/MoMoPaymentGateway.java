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
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoMoPaymentGateway implements PaymentGateway {

    private static final String MOMO_API_URL = "https://payment.momo.vn";
    private static final String MOMO_TEST_URL = "https://test-payment.momo.vn";
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${payment.momo.partner-code}")
    private String partnerCode;

    @Value("${payment.momo.access-key}")
    private String accessKey;

    @Value("${payment.momo.secret-key}")
    private String secretKey;

    @Value("${payment.momo.environment:test}")
    private String environment;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException {
        try {
            String requestId = UUID.randomUUID().toString();
            String orderId = "CARBON-" + request.getTransactionId();
            
            Map<String, Object> momoRequest = new HashMap<>();
            momoRequest.put("partnerCode", partnerCode);
            momoRequest.put("partnerName", "Carbon Credit Marketplace");
            momoRequest.put("storeId", "CARBON");
            momoRequest.put("requestId", requestId);
            momoRequest.put("amount", request.getAmount().longValue());
            momoRequest.put("orderId", orderId);
            momoRequest.put("orderInfo", request.getOrderInfo() != null ? request.getOrderInfo() : "Carbon Credit Purchase");
            momoRequest.put("redirectUrl", request.getRedirectUrl());
            momoRequest.put("ipnUrl", request.getCallbackUrl());
            momoRequest.put("lang", "vi");
            momoRequest.put("requestType", "captureWallet");
            momoRequest.put("autoCapture", true);
            momoRequest.put("extraData", "");
            
            // Generate signature
            String rawSignature = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey,
                momoRequest.get("amount"),
                momoRequest.get("extraData"),
                momoRequest.get("ipnUrl"),
                orderId,
                momoRequest.get("orderInfo"),
                partnerCode,
                momoRequest.get("redirectUrl"),
                requestId,
                momoRequest.get("requestType")
            );
            
            String signature = generateHmacSHA256(rawSignature, secretKey);
            momoRequest.put("signature", signature);
            
            // Call MoMo API
            String apiUrl = getApiUrl() + "/v2/gateway/api/create";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(momoRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if ("0".equals(String.valueOf(responseBody.get("resultCode")))) {
                    return PaymentResponse.builder()
                        .paymentId(UUID.randomUUID())
                        .transactionId(request.getTransactionId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .paymentMethod(request.getPaymentMethod())
                        .status(Payment.PaymentStatus.PENDING)
                        .paymentUrl(String.valueOf(responseBody.get("payUrl")))
                        .externalReference(requestId)
                        .gatewayTransactionId(orderId)
                        .qrCode(PaymentResponse.QRCodeInfo.builder()
                            .qrData(String.valueOf(responseBody.get("qrCodeUrl")))
                            .qrImageUrl(String.valueOf(responseBody.get("qrCodeUrl")))
                            .qrType("MOMO")
                            .build())
                        .expiresAt(LocalDateTime.now().plusMinutes(30))
                        .createdAt(LocalDateTime.now())
                        .message("Payment created successfully")
                        .build();
                } else {
                    throw new PaymentGatewayException(
                        "MoMo payment creation failed",
                        String.valueOf(responseBody.get("resultCode")),
                        String.valueOf(responseBody.get("message"))
                    );
                }
            }
            
            throw new PaymentGatewayException("Invalid response from MoMo gateway");
            
        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            throw new PaymentGatewayException("Failed to create MoMo payment", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(WebhookRequest request, String signature) {
        try {
            // MoMo webhook signature format
            String rawData = String.format(
                "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                accessKey,
                request.getAmount() != null ? request.getAmount().longValue() : "",
                request.getAdditionalProperties().getOrDefault("extraData", ""),
                request.getMessage() != null ? request.getMessage() : "",
                request.getOrderId() != null ? request.getOrderId() : "",
                request.getOrderInfo() != null ? request.getOrderInfo() : "",
                request.getOrderType() != null ? request.getOrderType() : "",
                partnerCode,
                request.getPayType() != null ? request.getPayType() : "",
                request.getRequestId() != null ? request.getRequestId() : "",
                request.getResponseTime() != null ? request.getResponseTime() : "",
                request.getResultCode() != null ? request.getResultCode() : "",
                request.getTransId() != null ? request.getTransId() : ""
            );
            
            String expectedSignature = generateHmacSHA256(rawData, secretKey);
            return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
            
        } catch (Exception e) {
            log.error("Error verifying MoMo webhook signature", e);
            return false;
        }
    }

    @Override
    public Payment processWebhook(WebhookRequest request) throws PaymentGatewayException {
        try {
            // Parse MoMo webhook response
            String resultCode = request.getResultCode();
            Payment.PaymentStatus status;
            
            switch (resultCode) {
                case "0":
                    status = Payment.PaymentStatus.COMPLETED;
                    break;
                case "1006":
                    status = Payment.PaymentStatus.EXPIRED;
                    break;
                case "1005":
                case "1017":
                    status = Payment.PaymentStatus.CANCELLED;
                    break;
                default:
                    status = Payment.PaymentStatus.FAILED;
            }
            
            Payment payment = new Payment();
            payment.setStatus(status);
            payment.setGatewayTransactionId(String.valueOf(request.getTransId()));
            payment.setExternalReference(request.getRequestId());
            payment.setGatewayResponse(objectMapper.writeValueAsString(request));
            
            if (status == Payment.PaymentStatus.COMPLETED) {
                payment.setPaidAt(LocalDateTime.now());
            } else if (status == Payment.PaymentStatus.FAILED) {
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason(request.getMessage());
            }
            
            return payment;
            
        } catch (Exception e) {
            log.error("Error processing MoMo webhook", e);
            throw new PaymentGatewayException("Failed to process MoMo webhook", e);
        }
    }

    @Override
    public Payment queryPaymentStatus(String transactionId) throws PaymentGatewayException {
        try {
            String requestId = UUID.randomUUID().toString();
            String orderId = "CARBON-" + transactionId;
            
            Map<String, Object> queryRequest = new HashMap<>();
            queryRequest.put("partnerCode", partnerCode);
            queryRequest.put("requestId", requestId);
            queryRequest.put("orderId", orderId);
            queryRequest.put("lang", "vi");
            
            // Generate signature for query
            String rawSignature = String.format(
                "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                accessKey,
                orderId,
                partnerCode,
                requestId
            );
            
            String signature = generateHmacSHA256(rawSignature, secretKey);
            queryRequest.put("signature", signature);
            
            // Call MoMo Query API
            String apiUrl = getApiUrl() + "/v2/gateway/api/query";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(queryRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                Payment payment = new Payment();
                String resultCode = String.valueOf(responseBody.get("resultCode"));
                
                if ("0".equals(resultCode)) {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaidAt(LocalDateTime.now());
                } else {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailedAt(LocalDateTime.now());
                    payment.setFailureReason(String.valueOf(responseBody.get("message")));
                }
                
                payment.setGatewayResponse(objectMapper.writeValueAsString(responseBody));
                return payment;
            }
            
            throw new PaymentGatewayException("Invalid response from MoMo query API");
            
        } catch (Exception e) {
            log.error("Error querying MoMo payment status", e);
            throw new PaymentGatewayException("Failed to query payment status", e);
        }
    }

    @Override
    public Payment processRefund(RefundRequest request) throws PaymentGatewayException {
        try {
            String requestId = UUID.randomUUID().toString();
            String orderId = "CARBON-" + request.getTransactionId();
            
            Map<String, Object> refundRequest = new HashMap<>();
            refundRequest.put("partnerCode", partnerCode);
            refundRequest.put("orderId", orderId);
            refundRequest.put("requestId", requestId);
            refundRequest.put("amount", request.getRefundAmount().longValue());
            refundRequest.put("transId", request.getPaymentId().toString());
            refundRequest.put("lang", "vi");
            refundRequest.put("description", request.getRefundReason());
            
            // Generate signature for refund
            String rawSignature = String.format(
                "accessKey=%s&amount=%d&description=%s&orderId=%s&partnerCode=%s&requestId=%s&transId=%s",
                accessKey,
                refundRequest.get("amount"),
                refundRequest.get("description"),
                orderId,
                partnerCode,
                requestId,
                refundRequest.get("transId")
            );
            
            String signature = generateHmacSHA256(rawSignature, secretKey);
            refundRequest.put("signature", signature);
            
            // Call MoMo Refund API
            String apiUrl = getApiUrl() + "/v2/gateway/api/refund";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(refundRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                Payment payment = new Payment();
                String resultCode = String.valueOf(responseBody.get("resultCode"));
                
                if ("0".equals(resultCode)) {
                    if (request.getRefundType() == RefundRequest.RefundType.FULL) {
                        payment.setStatus(Payment.PaymentStatus.REFUNDED);
                    } else {
                        payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
                    }
                } else {
                    throw new PaymentGatewayException(
                        "MoMo refund failed",
                        resultCode,
                        String.valueOf(responseBody.get("message"))
                    );
                }
                
                payment.setGatewayResponse(objectMapper.writeValueAsString(responseBody));
                return payment;
            }
            
            throw new PaymentGatewayException("Invalid response from MoMo refund API");
            
        } catch (Exception e) {
            log.error("Error processing MoMo refund", e);
            throw new PaymentGatewayException("Failed to process refund", e);
        }
    }

    @Override
    public boolean supports(Payment.PaymentMethod paymentMethod) {
        return Payment.PaymentMethod.MOMO == paymentMethod;
    }

    @Override
    public String getGatewayName() {
        return "MoMo";
    }

    private String getApiUrl() {
        return "production".equalsIgnoreCase(environment) ? MOMO_API_URL : MOMO_TEST_URL;
    }

    private String generateHmacSHA256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC SHA256", e);
        }
    }
}
