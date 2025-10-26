package com.carbonmarketplace.paymentservice.controller;

import com.carbonmarketplace.paymentservice.dto.WebhookRequest;
import com.carbonmarketplace.paymentservice.entity.PaymentWebhook;
import com.carbonmarketplace.paymentservice.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook", description = "Payment gateway webhook endpoints")
public class WebhookController {

    private final WebhookService webhookService;

    @Operation(summary = "MoMo webhook endpoint", description = "Receive payment notifications from MoMo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid webhook signature"),
        @ApiResponse(responseCode = "500", description = "Error processing webhook")
    })
    @PostMapping("/momo")
    public ResponseEntity<Map<String, String>> handleMoMoWebhook(
            @RequestBody WebhookRequest request,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            HttpServletRequest httpRequest) {
        try {
            log.info("Received MoMo webhook: {}", request.getOrderId());
            PaymentWebhook webhook = webhookService.processWebhook("MoMo", request, signature, httpRequest);
            
            if (webhook.getProcessingStatus() == PaymentWebhook.ProcessingStatus.PROCESSED) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook processed"));
            } else if (webhook.getProcessingStatus() == PaymentWebhook.ProcessingStatus.INVALID_SIGNATURE) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid signature"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Processing failed"));
            }
        } catch (Exception e) {
            log.error("Error processing MoMo webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @Operation(summary = "VNPay IPN endpoint", description = "Receive payment notifications from VNPay")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "IPN processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid IPN signature"),
        @ApiResponse(responseCode = "500", description = "Error processing IPN")
    })
    @PostMapping("/vnpay")
    public ResponseEntity<Map<String, String>> handleVNPayWebhook(
            @RequestBody WebhookRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Received VNPay IPN: {}", request.getVnpTxnRef());
            
            // VNPay sends signature in the request body
            String signature = request.getVnpSecureHash();
            PaymentWebhook webhook = webhookService.processWebhook("VNPay", request, signature, httpRequest);
            
            // VNPay expects specific response format
            if (webhook.getProcessingStatus() == PaymentWebhook.ProcessingStatus.PROCESSED) {
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "success"));
            } else if (webhook.getProcessingStatus() == PaymentWebhook.ProcessingStatus.INVALID_SIGNATURE) {
                return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
            } else if (webhook.getProcessingStatus() == PaymentWebhook.ProcessingStatus.DUPLICATE) {
                return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
            } else {
                return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", e.getMessage()));
        }
    }

    @Operation(summary = "Bank transfer webhook", description = "Receive bank transfer notifications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Error processing webhook")
    })
    @PostMapping("/bank-transfer")
    public ResponseEntity<Map<String, String>> handleBankTransferWebhook(
            @RequestBody WebhookRequest request,
            @RequestHeader(value = "X-Bank-Signature", required = false) String signature,
            @RequestHeader(value = "X-Bank-Secret", required = false) String secret,
            HttpServletRequest httpRequest) {
        try {
            log.info("Received bank transfer webhook: {}", request.getReferenceId());
            
            // Verify bank secret (simplified - in production use proper authentication)
            if (secret == null || !secret.equals("expected-bank-secret")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Unauthorized"));
            }
            
            PaymentWebhook webhook = webhookService.processWebhook("BankTransfer", request, signature, httpRequest);
            
            if (webhook.getProcessingStatus() == PaymentWebhook.ProcessingStatus.PROCESSED) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook processed"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Processing failed"));
            }
        } catch (Exception e) {
            log.error("Error processing bank transfer webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @Operation(summary = "Retry failed webhooks", description = "Manually trigger retry of failed webhooks (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retry initiated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/retry")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> retryFailedWebhooks() {
        try {
            webhookService.retryFailedWebhooks();
            return ResponseEntity.ok(Map.of("status", "success", "message", "Retry initiated"));
        } catch (Exception e) {
            log.error("Error retrying webhooks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
