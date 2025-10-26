package com.carbonmarketplace.transactionservice.client;

import com.carbonmarketplace.transactionservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for Payment Service
 */
@FeignClient(name = "payment-service", url = "${services.payment.url:http://payment-service:8083}")
public interface PaymentServiceClient {
    
    @PostMapping("/api/payments/process")
    PaymentResult processPayment(@RequestBody ProcessPaymentRequest request);
    
    @PostMapping("/api/payments/{paymentId}/refund")
    void refundPayment(@PathVariable UUID paymentId);
    
    @PostMapping("/api/payments/refund")
    String refundPayment(@RequestBody RefundPaymentRequest request);
    
    @GetMapping("/api/payments/{paymentId}/status")
    PaymentStatus getPaymentStatus(@PathVariable UUID paymentId);
    
    @PostMapping("/api/payouts/process")
    PayoutResult processPayout(@RequestBody PayoutRequest request);
}
