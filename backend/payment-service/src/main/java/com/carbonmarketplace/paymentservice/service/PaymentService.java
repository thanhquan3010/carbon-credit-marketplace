package com.carbonmarketplace.paymentservice.service;

import com.carbonmarketplace.paymentservice.dto.*;
import com.carbonmarketplace.paymentservice.entity.Payment;
import com.carbonmarketplace.paymentservice.entity.PaymentWebhook;
import com.carbonmarketplace.paymentservice.entity.Payout;
import com.carbonmarketplace.paymentservice.gateway.PaymentGateway;
import com.carbonmarketplace.paymentservice.gateway.PaymentGatewayException;
import com.carbonmarketplace.paymentservice.repository.PaymentRepository;
import com.carbonmarketplace.paymentservice.repository.PaymentWebhookRepository;
import com.carbonmarketplace.paymentservice.repository.PayoutRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookRepository webhookRepository;
    private final PayoutRepository payoutRepository;
    private final List<PaymentGateway> paymentGateways;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.platform.fee-percentage:5}")
    private BigDecimal platformFeePercentage;

    @Value("${payment.idempotency.ttl-hours:24}")
    private long idempotencyTtlHours;

    private static final String IDEMPOTENCY_KEY_PREFIX = "payment:idempotency:";
    private static final String PAYMENT_LOCK_PREFIX = "payment:lock:";

    /**
     * Create a payment with idempotency support
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) throws PaymentGatewayException {
        try {
            // Check idempotency key if provided
            if (request.getIdempotencyKey() != null) {
                PaymentResponse cachedResponse = checkIdempotencyKey(request.getIdempotencyKey());
                if (cachedResponse != null) {
                    log.info("Returning cached payment for idempotency key: {}", request.getIdempotencyKey());
                    return cachedResponse;
                }
            }

            // Generate idempotency key if not provided
            String idempotencyKey = request.getIdempotencyKey() != null 
                ? request.getIdempotencyKey() 
                : generateIdempotencyKey(request);

            // Acquire distributed lock
            String lockKey = PAYMENT_LOCK_PREFIX + request.getTransactionId();
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey, 
                UUID.randomUUID().toString(), 
                30, 
                TimeUnit.SECONDS
            );

            if (!Boolean.TRUE.equals(lockAcquired)) {
                throw new PaymentGatewayException("Payment already in progress for this transaction");
            }

            try {
                // Check if payment already exists for this transaction
                Optional<Payment> existingPayment = paymentRepository.findByTransactionId(request.getTransactionId());
                if (existingPayment.isPresent() && existingPayment.get().getStatus() != Payment.PaymentStatus.FAILED) {
                    throw new PaymentGatewayException("Payment already exists for this transaction");
                }

                // Find appropriate gateway
                PaymentGateway gateway = findGateway(request.getPaymentMethod());
                
                // Create payment with gateway
                PaymentResponse response = gateway.createPayment(request);
                
                // Calculate fees
                BigDecimal platformFee = calculatePlatformFee(request.getAmount());
                BigDecimal gatewayFee = calculateGatewayFee(request.getAmount(), request.getPaymentMethod());
                BigDecimal netAmount = request.getAmount().subtract(platformFee).subtract(gatewayFee);

                // Save payment to database
                Payment payment = Payment.builder()
                    .paymentId(response.getPaymentId())
                    .transactionId(request.getTransactionId())
                    .userId(request.getUserId())
                    .idempotencyKey(idempotencyKey)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .paymentMethod(request.getPaymentMethod())
                    .status(response.getStatus())
                    .externalReference(response.getExternalReference())
                    .gatewayTransactionId(response.getGatewayTransactionId())
                    .paymentUrl(response.getPaymentUrl())
                    .redirectUrl(request.getRedirectUrl())
                    .callbackUrl(request.getCallbackUrl())
                    .platformFee(platformFee)
                    .gatewayFee(gatewayFee)
                    .netAmount(netAmount)
                    .expiresAt(response.getExpiresAt())
                    .metadata(objectMapper.writeValueAsString(request.getMetadata()))
                    .build();

                // Handle virtual account for bank transfer
                if (response.getVirtualAccount() != null) {
                    payment.setVirtualAccountNumber(response.getVirtualAccount().getAccountNumber());
                    payment.setVirtualAccountName(response.getVirtualAccount().getAccountName());
                    payment.setBankCode(response.getVirtualAccount().getBankCode());
                }

                payment = paymentRepository.save(payment);
                log.info("Payment created: {}", payment.getPaymentId());

                // Cache response for idempotency
                cacheIdempotencyResponse(idempotencyKey, response);

                return response;

            } finally {
                // Release lock
                redisTemplate.delete(lockKey);
            }

        } catch (Exception e) {
            log.error("Error creating payment", e);
            if (e instanceof PaymentGatewayException) {
                throw (PaymentGatewayException) e;
            }
            throw new PaymentGatewayException("Failed to create payment", e);
        }
    }

    /**
     * Get payment by ID
     */
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new NoSuchElementException("Payment not found: " + paymentId));
    }

    /**
     * Get payment by transaction ID
     */
    public Payment getPaymentByTransaction(UUID transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new NoSuchElementException("Payment not found for transaction: " + transactionId));
    }

    /**
     * Query payment status from gateway
     */
    public Payment queryPaymentStatus(UUID paymentId) throws PaymentGatewayException {
        Payment payment = getPayment(paymentId);
        
        // Don't query if payment is already in final state
        if (isPaymentInFinalState(payment)) {
            return payment;
        }

        PaymentGateway gateway = findGateway(payment.getPaymentMethod());
        Payment updatedStatus = gateway.queryPaymentStatus(payment.getTransactionId().toString());
        
        // Update payment status
        payment.setStatus(updatedStatus.getStatus());
        payment.setGatewayResponse(updatedStatus.getGatewayResponse());
        
        if (updatedStatus.getStatus() == Payment.PaymentStatus.COMPLETED) {
            payment.setPaidAt(LocalDateTime.now());
        } else if (updatedStatus.getStatus() == Payment.PaymentStatus.FAILED) {
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason(updatedStatus.getFailureReason());
        }
        
        return paymentRepository.save(payment);
    }

    /**
     * Process refund
     */
    @Transactional
    public Payment processRefund(RefundRequest request) throws PaymentGatewayException {
        // Check idempotency for refund
        if (request.getIdempotencyKey() != null) {
            String refundIdempotencyKey = "refund:" + request.getIdempotencyKey();
            String cachedRefundId = redisTemplate.opsForValue().get(IDEMPOTENCY_KEY_PREFIX + refundIdempotencyKey);
            if (cachedRefundId != null) {
                log.info("Refund already processed with idempotency key: {}", request.getIdempotencyKey());
                return getPayment(UUID.fromString(cachedRefundId));
            }
        }

        Payment payment = getPayment(request.getPaymentId());
        
        // Validate refund amount
        if (request.getRefundAmount().compareTo(payment.getAmount()) > 0) {
            throw new PaymentGatewayException("Refund amount exceeds payment amount");
        }
        
        // Check if payment can be refunded
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new PaymentGatewayException("Only completed payments can be refunded");
        }
        
        // Process refund with gateway
        PaymentGateway gateway = findGateway(payment.getPaymentMethod());
        Payment refundResult = gateway.processRefund(request);
        
        // Update payment status
        payment.setStatus(refundResult.getStatus());
        payment.setGatewayResponse(refundResult.getGatewayResponse());
        payment = paymentRepository.save(payment);
        
        // Cache refund idempotency
        if (request.getIdempotencyKey() != null) {
            String refundIdempotencyKey = "refund:" + request.getIdempotencyKey();
            redisTemplate.opsForValue().set(
                IDEMPOTENCY_KEY_PREFIX + refundIdempotencyKey,
                payment.getPaymentId().toString(),
                idempotencyTtlHours,
                TimeUnit.HOURS
            );
        }
        
        log.info("Refund processed for payment: {}", payment.getPaymentId());
        return payment;
    }

    /**
     * Create payout for seller
     */
    @Transactional
    public Payout createPayout(Payment payment, UUID sellerId, BigDecimal creditAmount) {
        // Calculate payout amounts
        BigDecimal grossAmount = payment.getAmount();
        BigDecimal platformFee = payment.getPlatformFee();
        BigDecimal gatewayFee = payment.getGatewayFee();
        BigDecimal netAmount = grossAmount.subtract(platformFee).subtract(gatewayFee);
        
        Payout payout = Payout.builder()
            .transactionId(payment.getTransactionId())
            .paymentId(payment.getPaymentId())
            .sellerId(sellerId)
            .grossAmount(grossAmount)
            .platformFee(platformFee)
            .gatewayFee(gatewayFee)
            .netAmount(netAmount)
            .currency(payment.getCurrency())
            .status(Payout.PayoutStatus.SCHEDULED)
            .settlementDate(calculateSettlementDate())
            .build();
        
        return payoutRepository.save(payout);
    }

    /**
     * Process scheduled payouts
     */
    @Transactional
    public void processScheduledPayouts() {
        List<Payout> scheduledPayouts = payoutRepository.findPayoutsForSettlement(LocalDateTime.now());
        
        for (Payout payout : scheduledPayouts) {
            try {
                processPayout(payout);
            } catch (Exception e) {
                log.error("Error processing payout: {}", payout.getPayoutId(), e);
                payout.setStatus(Payout.PayoutStatus.FAILED);
                payout.setFailedAt(LocalDateTime.now());
                payout.setFailureReason(e.getMessage());
                payout.setRetryCount(payout.getRetryCount() + 1);
                payoutRepository.save(payout);
            }
        }
    }

    /**
     * Process expired payments
     */
    @Transactional
    public void processExpiredPayments() {
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(
            Payment.PaymentStatus.PENDING, 
            LocalDateTime.now()
        );
        
        for (Payment payment : expiredPayments) {
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason("Payment expired");
            paymentRepository.save(payment);
            log.info("Payment expired: {}", payment.getPaymentId());
        }
    }

    // Helper methods

    private PaymentGateway findGateway(Payment.PaymentMethod paymentMethod) throws PaymentGatewayException {
        return paymentGateways.stream()
            .filter(gateway -> gateway.supports(paymentMethod))
            .findFirst()
            .orElseThrow(() -> new PaymentGatewayException("No gateway found for payment method: " + paymentMethod));
    }

    private String generateIdempotencyKey(PaymentRequest request) {
        return String.format("%s-%s-%s", 
            request.getUserId(), 
            request.getTransactionId(), 
            System.currentTimeMillis()
        );
    }

    private PaymentResponse checkIdempotencyKey(String idempotencyKey) {
        try {
            String cachedResponse = redisTemplate.opsForValue().get(IDEMPOTENCY_KEY_PREFIX + idempotencyKey);
            if (cachedResponse != null) {
                return objectMapper.readValue(cachedResponse, PaymentResponse.class);
            }
        } catch (Exception e) {
            log.error("Error checking idempotency key", e);
        }
        return null;
    }

    private void cacheIdempotencyResponse(String idempotencyKey, PaymentResponse response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                responseJson,
                idempotencyTtlHours,
                TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.error("Error caching idempotency response", e);
        }
    }

    private BigDecimal calculatePlatformFee(BigDecimal amount) {
        return amount.multiply(platformFeePercentage).divide(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateGatewayFee(BigDecimal amount, Payment.PaymentMethod method) {
        // Gateway fee percentages (can be moved to configuration)
        Map<Payment.PaymentMethod, BigDecimal> gatewayFees = Map.of(
            Payment.PaymentMethod.MOMO, new BigDecimal("1.5"),
            Payment.PaymentMethod.VNPAY, new BigDecimal("1.8"),
            Payment.PaymentMethod.ZALOPAY, new BigDecimal("1.7"),
            Payment.PaymentMethod.BANK_TRANSFER, BigDecimal.ZERO,
            Payment.PaymentMethod.STRIPE, new BigDecimal("2.9")
        );
        
        BigDecimal feePercentage = gatewayFees.getOrDefault(method, BigDecimal.ZERO);
        return amount.multiply(feePercentage).divide(BigDecimal.valueOf(100));
    }

    private LocalDateTime calculateSettlementDate() {
        // T+2 business days
        LocalDateTime settlementDate = LocalDateTime.now().plusDays(2);
        // Skip weekends (simplified - in production, should consider holidays)
        while (settlementDate.getDayOfWeek().getValue() > 5) {
            settlementDate = settlementDate.plusDays(1);
        }
        return settlementDate;
    }

    private boolean isPaymentInFinalState(Payment payment) {
        return payment.getStatus() == Payment.PaymentStatus.COMPLETED ||
               payment.getStatus() == Payment.PaymentStatus.FAILED ||
               payment.getStatus() == Payment.PaymentStatus.CANCELLED ||
               payment.getStatus() == Payment.PaymentStatus.REFUNDED ||
               payment.getStatus() == Payment.PaymentStatus.EXPIRED;
    }

    private void processPayout(Payout payout) {
        // In production, this would integrate with banking APIs
        // For now, we'll simulate the payout process
        payout.setStatus(Payout.PayoutStatus.PROCESSING);
        payoutRepository.save(payout);
        
        // Simulate bank transfer
        // In reality, this would call bank API
        
        payout.setStatus(Payout.PayoutStatus.COMPLETED);
        payout.setProcessedAt(LocalDateTime.now());
        payout.setReferenceNumber(generatePayoutReference());
        payoutRepository.save(payout);
        
        log.info("Payout processed: {}", payout.getPayoutId());
    }

    private String generatePayoutReference() {
        return "PO-" + System.currentTimeMillis();
    }
}
