package com.carbonmarketplace.paymentservice.gateway;

import com.carbonmarketplace.paymentservice.dto.PaymentRequest;
import com.carbonmarketplace.paymentservice.dto.PaymentResponse;
import com.carbonmarketplace.paymentservice.dto.RefundRequest;
import com.carbonmarketplace.paymentservice.dto.WebhookRequest;
import com.carbonmarketplace.paymentservice.entity.Payment;

public interface PaymentGateway {
    
    /**
     * Create a payment with the gateway
     */
    PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException;
    
    /**
     * Verify webhook signature
     */
    boolean verifyWebhookSignature(WebhookRequest request, String signature);
    
    /**
     * Process webhook notification
     */
    Payment processWebhook(WebhookRequest request) throws PaymentGatewayException;
    
    /**
     * Query payment status from gateway
     */
    Payment queryPaymentStatus(String transactionId) throws PaymentGatewayException;
    
    /**
     * Process refund
     */
    Payment processRefund(RefundRequest request) throws PaymentGatewayException;
    
    /**
     * Check if this gateway handles the given payment method
     */
    boolean supports(Payment.PaymentMethod paymentMethod);
    
    /**
     * Get gateway name
     */
    String getGatewayName();
}
