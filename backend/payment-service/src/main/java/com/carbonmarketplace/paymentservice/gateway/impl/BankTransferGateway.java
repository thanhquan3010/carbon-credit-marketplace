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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransferGateway implements PaymentGateway {

    private static final String BANK_NAME = "VietcomBank";
    private static final String BANK_CODE = "VCB";
    private static final String ACCOUNT_PREFIX = "999";
    private static final int VIRTUAL_ACCOUNT_LENGTH = 13;
    
    @Value("${payment.bank.account-name:Carbon Credit Marketplace}")
    private String accountName;
    
    @Value("${payment.bank.master-account:1234567890}")
    private String masterAccount;
    
    @Value("${payment.bank.transfer-timeout-hours:48}")
    private int transferTimeoutHours;

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException {
        try {
            // Generate virtual account number
            String virtualAccountNumber = generateVirtualAccountNumber(request.getTransactionId());
            String referenceCode = generateReferenceCode(request.getTransactionId());
            
            // Create payment instructions
            String paymentInstructions = buildPaymentInstructions(
                virtualAccountNumber,
                referenceCode,
                request.getAmount()
            );
            
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(transferTimeoutHours);
            
            return PaymentResponse.builder()
                .paymentId(UUID.randomUUID())
                .transactionId(request.getTransactionId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .virtualAccount(PaymentResponse.VirtualAccountInfo.builder()
                    .accountNumber(virtualAccountNumber)
                    .accountName(accountName)
                    .bankCode(BANK_CODE)
                    .bankName(BANK_NAME)
                    .referenceCode(referenceCode)
                    .build())
                .externalReference(referenceCode)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .message(paymentInstructions)
                .build();
                
        } catch (Exception e) {
            log.error("Error creating bank transfer payment", e);
            throw new PaymentGatewayException("Failed to create bank transfer payment", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(WebhookRequest request, String signature) {
        // Bank transfer webhooks would typically come from the bank's API
        // For now, we'll implement a basic signature verification
        // In production, this would verify against the bank's signature mechanism
        try {
            if (signature == null || signature.isEmpty()) {
                return false;
            }
            
            // Verify the webhook is from a trusted source
            // This is a simplified implementation
            return signature.startsWith("BANK_") && signature.length() > 20;
            
        } catch (Exception e) {
            log.error("Error verifying bank transfer webhook signature", e);
            return false;
        }
    }

    @Override
    public Payment processWebhook(WebhookRequest request) throws PaymentGatewayException {
        try {
            // Process bank transfer notification
            // This would typically parse the bank's specific webhook format
            Payment payment = new Payment();
            
            // Check if payment was successful based on webhook data
            String status = request.getStatus();
            if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
            } else if ("FAILED".equalsIgnoreCase(status)) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason(request.getMessage());
            } else {
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
            }
            
            payment.setExternalReference(request.getReferenceId());
            payment.setGatewayResponse(objectMapper.writeValueAsString(request));
            
            return payment;
            
        } catch (Exception e) {
            log.error("Error processing bank transfer webhook", e);
            throw new PaymentGatewayException("Failed to process bank transfer webhook", e);
        }
    }

    @Override
    public Payment queryPaymentStatus(String transactionId) throws PaymentGatewayException {
        try {
            // In a real implementation, this would query the bank's API
            // For now, we'll return a mock response
            log.info("Querying bank transfer status for transaction: {}", transactionId);
            
            Payment payment = new Payment();
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setExternalReference(generateReferenceCode(UUID.fromString(transactionId)));
            
            // Simulate checking with bank API
            // In production, this would make an actual API call to the bank
            payment.setGatewayResponse("{\"status\":\"PENDING\",\"message\":\"Awaiting bank confirmation\"}");
            
            return payment;
            
        } catch (Exception e) {
            log.error("Error querying bank transfer status", e);
            throw new PaymentGatewayException("Failed to query payment status", e);
        }
    }

    @Override
    public Payment processRefund(RefundRequest request) throws PaymentGatewayException {
        try {
            // Bank transfer refunds typically require manual processing
            // or integration with the bank's refund API
            log.info("Processing bank transfer refund for payment: {}", request.getPaymentId());
            
            Payment payment = new Payment();
            
            // In a real implementation, this would initiate a bank transfer refund
            // For now, we'll mark it as processing
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment.setGatewayResponse(objectMapper.writeValueAsString(request));
            
            // Refund would be processed asynchronously
            // Bank would send a webhook when refund is complete
            
            return payment;
            
        } catch (Exception e) {
            log.error("Error processing bank transfer refund", e);
            throw new PaymentGatewayException("Failed to process refund", e);
        }
    }

    @Override
    public boolean supports(Payment.PaymentMethod paymentMethod) {
        return Payment.PaymentMethod.BANK_TRANSFER == paymentMethod;
    }

    @Override
    public String getGatewayName() {
        return "Bank Transfer";
    }

    /**
     * Generate a unique virtual account number for the transaction
     */
    private String generateVirtualAccountNumber(UUID transactionId) {
        // Format: PREFIX + YYMMDD + RANDOM(4)
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomStr = String.format("%04d", secureRandom.nextInt(10000));
        
        String virtualAccount = ACCOUNT_PREFIX + dateStr + randomStr;
        
        // Ensure it's exactly the required length
        if (virtualAccount.length() < VIRTUAL_ACCOUNT_LENGTH) {
            virtualAccount = virtualAccount + generateRandomDigits(VIRTUAL_ACCOUNT_LENGTH - virtualAccount.length());
        } else if (virtualAccount.length() > VIRTUAL_ACCOUNT_LENGTH) {
            virtualAccount = virtualAccount.substring(0, VIRTUAL_ACCOUNT_LENGTH);
        }
        
        return virtualAccount;
    }

    /**
     * Generate a reference code for the transaction
     */
    private String generateReferenceCode(UUID transactionId) {
        // Format: CARBON-YYMMDD-XXXX (where XXXX is from transaction ID)
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String txnIdShort = transactionId.toString().substring(0, 4).toUpperCase();
        return String.format("CARBON-%s-%s", dateStr, txnIdShort);
    }

    /**
     * Build payment instructions for the customer
     */
    private String buildPaymentInstructions(String virtualAccount, String referenceCode, BigDecimal amount) {
        return String.format(
            "Please transfer %s %s to account %s at %s. " +
            "Account name: %s. " +
            "Reference/Content: %s. " +
            "Important: Include the exact reference code in your transfer description. " +
            "Your payment will be automatically confirmed within 15 minutes after successful transfer.",
            amount.toPlainString(),
            "VND",
            virtualAccount,
            BANK_NAME,
            accountName,
            referenceCode
        );
    }

    /**
     * Generate random digits
     */
    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
