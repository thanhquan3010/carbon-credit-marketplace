package com.carbonmarketplace.paymentservice.service;

import com.carbonmarketplace.paymentservice.dto.WebhookRequest;
import com.carbonmarketplace.paymentservice.entity.Payment;
import com.carbonmarketplace.paymentservice.entity.PaymentWebhook;
import com.carbonmarketplace.paymentservice.gateway.PaymentGateway;
import com.carbonmarketplace.paymentservice.gateway.PaymentGatewayException;
import com.carbonmarketplace.paymentservice.repository.PaymentRepository;
import com.carbonmarketplace.paymentservice.repository.PaymentWebhookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentWebhookRepository webhookRepository;
    private final PaymentRepository paymentRepository;
    private final List<PaymentGateway> paymentGateways;
    private final ObjectMapper objectMapper;

    /**
     * Process webhook from payment gateway
     */
    @Transactional
    public PaymentWebhook processWebhook(String gateway, WebhookRequest request, String signature, HttpServletRequest httpRequest) {
        try {
            // Save webhook for audit
            PaymentWebhook webhook = saveWebhook(gateway, request, signature, httpRequest);
            
            // Check for duplicate webhook
            if (isDuplicateWebhook(signature)) {
                log.warn("Duplicate webhook received: {}", signature);
                webhook.setProcessingStatus(PaymentWebhook.ProcessingStatus.DUPLICATE);
                return webhookRepository.save(webhook);
            }
            
            // Find appropriate gateway
            PaymentGateway paymentGateway = findGatewayByName(gateway);
            if (paymentGateway == null) {
                log.error("Unknown gateway: {}", gateway);
                webhook.setProcessingStatus(PaymentWebhook.ProcessingStatus.FAILED);
                webhook.setProcessingError("Unknown gateway: " + gateway);
                return webhookRepository.save(webhook);
            }
            
            // Verify signature
            boolean isValidSignature = paymentGateway.verifyWebhookSignature(request, signature);
            webhook.setIsValidSignature(isValidSignature);
            
            if (!isValidSignature) {
                log.error("Invalid webhook signature from gateway: {}", gateway);
                webhook.setProcessingStatus(PaymentWebhook.ProcessingStatus.INVALID_SIGNATURE);
                webhook.setProcessingError("Invalid signature");
                return webhookRepository.save(webhook);
            }
            
            // Process webhook with gateway
            webhook.setProcessingStatus(PaymentWebhook.ProcessingStatus.PROCESSING);
            webhookRepository.save(webhook);
            
            Payment paymentUpdate = paymentGateway.processWebhook(request);
            
            // Find and update payment
            Payment payment = findPaymentFromWebhook(request);
            if (payment != null) {
                updatePaymentFromWebhook(payment, paymentUpdate);
                webhook.setPaymentId(payment.getPaymentId());
            } else {
                log.warn("Payment not found for webhook: {}", objectMapper.writeValueAsString(request));
            }
            
            // Mark webhook as processed
            webhook.setProcessingStatus(PaymentWebhook.ProcessingStatus.PROCESSED);
            webhook.setProcessedAt(LocalDateTime.now());
            webhook.setResponseCode(200);
            webhook.setResponseBody("{\"status\":\"success\"}");
            
            return webhookRepository.save(webhook);
            
        } catch (Exception e) {
            log.error("Error processing webhook from gateway: {}", gateway, e);
            
            // Save failed webhook
            PaymentWebhook webhook = PaymentWebhook.builder()
                .gateway(gateway)
                .requestBody(getRequestBodySafe(request))
                .signature(signature)
                .processingStatus(PaymentWebhook.ProcessingStatus.FAILED)
                .processingError(e.getMessage())
                .responseCode(500)
                .responseBody("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}")
                .build();
            
            return webhookRepository.save(webhook);
        }
    }

    /**
     * Retry failed webhooks
     */
    @Transactional
    public void retryFailedWebhooks() {
        List<PaymentWebhook> failedWebhooks = webhookRepository.findFailedWebhooksForRetry(3);
        
        for (PaymentWebhook webhook : failedWebhooks) {
            try {
                log.info("Retrying webhook: {}", webhook.getWebhookId());
                
                WebhookRequest request = objectMapper.readValue(webhook.getRequestBody(), WebhookRequest.class);
                PaymentGateway gateway = findGatewayByName(webhook.getGateway());
                
                if (gateway != null) {
                    Payment paymentUpdate = gateway.processWebhook(request);
                    Payment payment = findPaymentFromWebhook(request);
                    
                    if (payment != null) {
                        updatePaymentFromWebhook(payment, paymentUpdate);
                        webhook.setPaymentId(payment.getPaymentId());
                        webhook.setProcessingStatus(PaymentWebhook.ProcessingStatus.PROCESSED);
                        webhook.setProcessedAt(LocalDateTime.now());
                    }
                }
                
                webhook.setRetryCount(webhook.getRetryCount() + 1);
                webhookRepository.save(webhook);
                
            } catch (Exception e) {
                log.error("Failed to retry webhook: {}", webhook.getWebhookId(), e);
                webhook.setRetryCount(webhook.getRetryCount() + 1);
                webhook.setProcessingError(e.getMessage());
                webhookRepository.save(webhook);
            }
        }
    }

    // Helper methods

    private PaymentWebhook saveWebhook(String gateway, WebhookRequest request, String signature, HttpServletRequest httpRequest) {
        try {
            Map<String, String> headers = extractHeaders(httpRequest);
            
            PaymentWebhook webhook = PaymentWebhook.builder()
                .gateway(gateway)
                .eventType(request.getAdditionalProperties().getOrDefault("event_type", "payment").toString())
                .requestHeaders(objectMapper.writeValueAsString(headers))
                .requestBody(objectMapper.writeValueAsString(request))
                .signature(signature)
                .processingStatus(PaymentWebhook.ProcessingStatus.RECEIVED)
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();
            
            return webhookRepository.save(webhook);
            
        } catch (Exception e) {
            log.error("Error saving webhook", e);
            throw new RuntimeException("Failed to save webhook", e);
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private boolean isDuplicateWebhook(String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        
        // Check if we've seen this signature in the last hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Optional<PaymentWebhook> duplicate = webhookRepository.findDuplicateWebhook(signature, oneHourAgo);
        return duplicate.isPresent();
    }

    private PaymentGateway findGatewayByName(String gatewayName) {
        return paymentGateways.stream()
            .filter(gateway -> gateway.getGatewayName().equalsIgnoreCase(gatewayName))
            .findFirst()
            .orElse(null);
    }

    private Payment findPaymentFromWebhook(WebhookRequest request) {
        // Try different fields to find the payment
        
        // MoMo uses orderId
        if (request.getOrderId() != null && request.getOrderId().startsWith("CARBON-")) {
            String transactionId = request.getOrderId().replace("CARBON-", "");
            try {
                return paymentRepository.findByTransactionId(UUID.fromString(transactionId)).orElse(null);
            } catch (Exception e) {
                log.debug("Could not parse transaction ID from order ID: {}", request.getOrderId());
            }
        }
        
        // VNPay uses vnp_TxnRef
        if (request.getVnpTxnRef() != null && request.getVnpTxnRef().startsWith("CARBON-")) {
            String transactionId = request.getVnpTxnRef().replace("CARBON-", "");
            try {
                return paymentRepository.findByTransactionId(UUID.fromString(transactionId)).orElse(null);
            } catch (Exception e) {
                log.debug("Could not parse transaction ID from VNPay TxnRef: {}", request.getVnpTxnRef());
            }
        }
        
        // Try external reference
        if (request.getReferenceId() != null) {
            return paymentRepository.findByExternalReference(request.getReferenceId()).orElse(null);
        }
        
        // Try gateway transaction ID
        if (request.getTransactionId() != null) {
            return paymentRepository.findByGatewayTransactionId(request.getTransactionId()).orElse(null);
        }
        
        return null;
    }

    private void updatePaymentFromWebhook(Payment payment, Payment webhookUpdate) {
        payment.setStatus(webhookUpdate.getStatus());
        payment.setGatewayResponse(webhookUpdate.getGatewayResponse());
        
        if (webhookUpdate.getGatewayTransactionId() != null) {
            payment.setGatewayTransactionId(webhookUpdate.getGatewayTransactionId());
        }
        
        if (webhookUpdate.getStatus() == Payment.PaymentStatus.COMPLETED) {
            payment.setPaidAt(webhookUpdate.getPaidAt() != null ? webhookUpdate.getPaidAt() : LocalDateTime.now());
        } else if (webhookUpdate.getStatus() == Payment.PaymentStatus.FAILED) {
            payment.setFailedAt(webhookUpdate.getFailedAt() != null ? webhookUpdate.getFailedAt() : LocalDateTime.now());
            payment.setFailureReason(webhookUpdate.getFailureReason());
        }
        
        paymentRepository.save(payment);
        log.info("Payment updated from webhook: {} -> {}", payment.getPaymentId(), payment.getStatus());
    }

    private String getRequestBodySafe(WebhookRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            return "{\"error\":\"Could not serialize request\"}";
        }
    }
}
