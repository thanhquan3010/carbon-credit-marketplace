package com.carbonmarketplace.transactionservice.service;

import com.carbonmarketplace.transactionservice.dto.*;
import com.carbonmarketplace.transactionservice.dto.TransactionDTOs.*;
import com.carbonmarketplace.transactionservice.entity.*;
import com.carbonmarketplace.transactionservice.exception.SagaExecutionException;
import com.carbonmarketplace.transactionservice.repository.*;
import com.carbonmarketplace.transactionservice.saga.SagaOrchestrator;
import com.carbonmarketplace.transactionservice.saga.SagaTransaction;
import com.carbonmarketplace.transactionservice.client.PaymentServiceClient;
import com.carbonmarketplace.transactionservice.client.CarbonCreditServiceClient;
import com.carbonmarketplace.transactionservice.client.CertificateServiceClient;
import com.carbonmarketplace.transactionservice.client.NotificationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transaction Saga Service
 * 
 * Implements the Saga pattern for distributed transaction management across
 * microservices.
 * Handles transaction orchestration, compensation, and failure recovery.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionSagaService {

    private final TransactionRepository transactionRepository;
    private final EscrowAccountRepository escrowAccountRepository;
    private final SagaStepRepository sagaStepRepository;
    private final EscrowService escrowService;
    private final SagaOrchestrator sagaOrchestrator;

    // Feign clients for external services
    private final PaymentServiceClient paymentServiceClient;
    private final CarbonCreditServiceClient carbonCreditServiceClient;
    private final CertificateServiceClient certificateServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Value("${transaction.fees.platform-percentage:5.0}")
    private BigDecimal platformFeePercentage;

    @Value("${transaction.saga.timeout-seconds:300}")
    private Integer sagaTimeoutSeconds;

    @Value("${transaction.saga.max-retry-attempts:3}")
    private Integer maxRetryAttempts;

    /**
     * Execute purchase transaction using Saga pattern
     */
    public TransactionResult executePurchase(PurchaseRequest request) {
        log.info("Starting purchase transaction for listing: {}", request.getListingId());

        // Create saga transaction
        String sagaId = UUID.randomUUID().toString();
        SagaTransaction saga = new SagaTransaction(sagaId, sagaTimeoutSeconds);

        final Transaction[] transactionHolder = { null };
        final CreditLockResult[] creditLockHolder = { null };
        final EscrowAccount[] escrowHolder = { null };
        final PaymentResult[] paymentHolder = { null };
        final CreditTransferResult[] transferHolder = { null };
        final CertificateResult[] certificateHolder = { null };
        final SettlementScheduleResult[] settlementHolder = { null };

        try {
            // Step 1: Create transaction record
            transactionHolder[0] = saga.execute(
                    "CREATE_TRANSACTION",
                    () -> createTransaction(request),
                    () -> deleteTransaction(request.getIdempotencyKey()));

            recordSagaStep(transactionHolder[0], sagaId, "CREATE_TRANSACTION", 1,
                    SagaStep.StepStatus.COMPLETED);

            // Step 2: Validate and lock credits
            creditLockHolder[0] = saga.execute(
                    "LOCK_CREDITS",
                    () -> lockCreditsInInventory(transactionHolder[0]),
                    () -> {
                    }); // Compensation handled in compensateTransaction()

            recordSagaStep(transactionHolder[0], sagaId, "LOCK_CREDITS", 2,
                    SagaStep.StepStatus.COMPLETED);

            // Step 3: Create escrow account
            escrowHolder[0] = saga.execute(
                    "CREATE_ESCROW",
                    () -> createEscrowAccount(transactionHolder[0], creditLockHolder[0].getLockId()),
                    () -> {
                    }); // Compensation handled in compensateTransaction()

            transactionHolder[0].setEscrowAccount(escrowHolder[0]);
            recordSagaStep(transactionHolder[0], sagaId, "CREATE_ESCROW", 3,
                    SagaStep.StepStatus.COMPLETED);

            // Step 4: Process payment
            paymentHolder[0] = saga.execute(
                    "PROCESS_PAYMENT",
                    () -> processPayment(transactionHolder[0]),
                    () -> {
                    }); // Compensation handled in compensateTransaction()

            transactionHolder[0].setPaymentId(paymentHolder[0].getPaymentId());
            transactionHolder[0].setPaymentStatus(Transaction.PaymentStatus.PROCESSING);
            transactionHolder[0].setPaymentInitiatedAt(LocalDateTime.now());
            transactionRepository.save(transactionHolder[0]);

            recordSagaStep(transactionHolder[0], sagaId, "PROCESS_PAYMENT", 4,
                    SagaStep.StepStatus.COMPLETED);

            // Step 5: Wait for payment confirmation (async)
            boolean paymentConfirmed = waitForPaymentConfirmation(paymentHolder[0].getPaymentId());

            if (!paymentConfirmed) {
                throw new SagaExecutionException("Payment confirmation timeout");
            }

            transactionHolder[0].setPaymentStatus(Transaction.PaymentStatus.COMPLETED);
            transactionHolder[0].setPaymentCompletedAt(LocalDateTime.now());

            // Step 6: Hold funds in escrow
            saga.execute(
                    "HOLD_IN_ESCROW",
                    () -> {
                        holdFundsInEscrow(escrowHolder[0], paymentHolder[0]);
                        return null;
                    },
                    () -> {
                    }); // Compensation handled in compensateTransaction()

            recordSagaStep(transactionHolder[0], sagaId, "HOLD_IN_ESCROW", 5,
                    SagaStep.StepStatus.COMPLETED);

            // Step 7: Transfer credits to buyer
            transferHolder[0] = saga.execute(
                    "TRANSFER_CREDITS",
                    () -> transferCreditsToBuyer(transactionHolder[0], creditLockHolder[0]),
                    () -> {
                    }); // Compensation handled in compensateTransaction()

            transactionHolder[0].setCreditsTransferredAt(LocalDateTime.now());
            recordSagaStep(transactionHolder[0], sagaId, "TRANSFER_CREDITS", 6,
                    SagaStep.StepStatus.COMPLETED);

            // Step 8: Generate certificate
            certificateHolder[0] = saga.execute(
                    "GENERATE_CERTIFICATE",
                    () -> generateCertificate(transactionHolder[0], transferHolder[0]),
                    () -> {
                    }); // Compensation handled in compensateTransaction()

            transactionHolder[0].setCertificateId(certificateHolder[0].getCertificateId());
            transactionHolder[0].setCertificateIssuedAt(LocalDateTime.now());
            recordSagaStep(transactionHolder[0], sagaId, "GENERATE_CERTIFICATE", 7,
                    SagaStep.StepStatus.COMPLETED);

            // Step 9: Schedule settlement (T+2)
            settlementHolder[0] = saga.execute(
                    "SCHEDULE_SETTLEMENT",
                    () -> scheduleSettlement(transactionHolder[0], escrowHolder[0]),
                    () -> {
                    }); // Compensation handled in compensateTransaction()

            transactionHolder[0].setSettlementDate(settlementHolder[0].getScheduledDate());
            transactionHolder[0].setStatus(Transaction.TransactionStatus.IN_SETTLEMENT);
            recordSagaStep(transactionHolder[0], sagaId, "SCHEDULE_SETTLEMENT", 8,
                    SagaStep.StepStatus.COMPLETED);

            // Step 10: Send notifications (no compensation needed)
            saga.execute(
                    "SEND_NOTIFICATIONS",
                    () -> {
                        sendTransactionNotifications(transactionHolder[0], certificateHolder[0]);
                        return null;
                    },
                    () -> {
                    }); // No compensation for notifications

            recordSagaStep(transactionHolder[0], sagaId, "SEND_NOTIFICATIONS", 9,
                    SagaStep.StepStatus.COMPLETED);

            // Commit saga
            saga.commit();

            // Update final transaction status
            transactionHolder[0].setStatus(Transaction.TransactionStatus.COMPLETED);
            transactionHolder[0].setCompletedAt(LocalDateTime.now());
            transactionHolder[0] = transactionRepository.save(transactionHolder[0]);

            log.info("Purchase transaction completed successfully: {}",
                    transactionHolder[0].getTransactionId());

            return TransactionResult.success(transactionHolder[0]);

        } catch (Exception e) {
            log.error("Purchase transaction failed: {}", e.getMessage(), e);

            // Rollback saga
            saga.rollback();

            if (transactionHolder[0] != null) {
                transactionHolder[0].setStatus(Transaction.TransactionStatus.FAILED);
                transactionHolder[0].setFailureReason(e.getMessage());
                transactionHolder[0].setFailedAt(LocalDateTime.now());
                transactionRepository.save(transactionHolder[0]);

                // Record failed saga steps
                recordSagaFailure(transactionHolder[0], sagaId, e.getMessage());
            }

            return TransactionResult.failure(e.getMessage());
        }
    }

    /**
     * Create initial transaction record
     */
    private Transaction createTransaction(PurchaseRequest request) {
        log.debug("Creating transaction for listing: {}", request.getListingId());

        // Check for duplicate using idempotency key
        if (request.getIdempotencyKey() != null) {
            transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Duplicate transaction: " +
                                existing.getTransactionId());
                    });
        }

        // Calculate fees
        BigDecimal totalAmount = request.getUnitPrice()
                .multiply(request.getCreditAmount());
        BigDecimal platformFee = totalAmount.multiply(platformFeePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sellerReceives = totalAmount.subtract(platformFee);

        Transaction transaction = Transaction.builder()
                .listingId(request.getListingId())
                .buyerId(request.getBuyerId())
                .sellerId(request.getSellerId())
                .creditAmountTons(request.getCreditAmount())
                .unitPriceVnd(request.getUnitPrice())
                .totalAmountVnd(totalAmount)
                .platformFeeVnd(platformFee)
                .sellerReceivesVnd(sellerReceives)
                .status(Transaction.TransactionStatus.PENDING)
                .paymentStatus(Transaction.PaymentStatus.PENDING)
                .escrowStatus(Transaction.EscrowStatus.PENDING)
                .buyerName(request.getBuyerName())
                .buyerEmail(request.getBuyerEmail())
                .buyerPhone(request.getBuyerPhone())
                .buyerCompany(request.getBuyerCompany())
                .buyerTaxId(request.getBuyerTaxId())
                .buyerAddress(request.getBuyerAddress())
                .idempotencyKey(request.getIdempotencyKey())
                .isExpressSettlement(request.getIsExpressSettlement())
                .notes(request.getNotes())
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Delete transaction (compensation)
     */
    private void deleteTransaction(String idempotencyKey) {
        log.debug("Deleting transaction with idempotency key: {}", idempotencyKey);

        transactionRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(transaction -> {
                    transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
                    transaction.setCancelledAt(LocalDateTime.now());
                    transactionRepository.save(transaction);
                });
    }

    /**
     * Lock credits in inventory
     */
    private CreditLockResult lockCreditsInInventory(Transaction transaction) {
        log.debug("Locking {} tons of credits for transaction: {}",
                transaction.getCreditAmountTons(), transaction.getTransactionId());

        return carbonCreditServiceClient.lockCredits(
                LockCreditsRequest.builder()
                        .listingId(transaction.getListingId())
                        .amount(transaction.getCreditAmountTons())
                        .transactionId(transaction.getTransactionId())
                        .build());
    }

    /**
     * Unlock credits (compensation)
     */
    private void unlockCreditsInInventory(String lockId) {
        log.debug("Unlocking credits with lock ID: {}", lockId);

        carbonCreditServiceClient.unlockCredits(lockId);
    }

    /**
     * Create escrow account
     */
    private EscrowAccount createEscrowAccount(Transaction transaction, String creditLockId) {
        return escrowService.createEscrowAccount(transaction, creditLockId);
    }

    /**
     * Delete escrow account (compensation)
     */
    private void deleteEscrowAccount(UUID escrowAccountId) {
        escrowService.cancelEscrowAccount(escrowAccountId);
    }

    /**
     * Process payment through payment service
     */
    private PaymentResult processPayment(Transaction transaction) {
        log.debug("Processing payment for transaction: {}", transaction.getTransactionId());

        return paymentServiceClient.processPayment(
                ProcessPaymentRequest.builder()
                        .transactionId(transaction.getTransactionId())
                        .amount(transaction.getTotalAmountVnd())
                        .currency("VND")
                        .buyerId(transaction.getBuyerId())
                        .description("Carbon Credit Purchase - " + transaction.getCreditAmountTons() + " tons")
                        .build());
    }

    /**
     * Refund payment (compensation)
     */
    private void refundPayment(UUID paymentId) {
        log.debug("Refunding payment: {}", paymentId);

        paymentServiceClient.refundPayment(paymentId);
    }

    /**
     * Wait for payment confirmation
     */
    private boolean waitForPaymentConfirmation(UUID paymentId) {
        // Implementation would typically involve polling or webhook
        // For now, simulate with a simple check
        return true;
    }

    /**
     * Hold funds in escrow
     */
    private void holdFundsInEscrow(EscrowAccount escrow, PaymentResult payment) {
        escrowService.holdFunds(escrow.getEscrowAccountId(), payment.getPaymentId());
    }

    /**
     * Release escrow hold (compensation)
     */
    private void releaseEscrowHold(UUID escrowAccountId) {
        escrowService.releaseFunds(escrowAccountId, "COMPENSATION");
    }

    /**
     * Transfer credits to buyer
     */
    private CreditTransferResult transferCreditsToBuyer(Transaction transaction,
            CreditLockResult creditLock) {
        log.debug("Transferring credits to buyer: {}", transaction.getBuyerId());

        return carbonCreditServiceClient.transferCredits(
                TransferCreditsRequest.builder()
                        .fromUserId(transaction.getSellerId())
                        .toUserId(transaction.getBuyerId())
                        .amount(transaction.getCreditAmountTons())
                        .lockId(creditLock.getLockId())
                        .transactionId(transaction.getTransactionId())
                        .build());
    }

    /**
     * Reverse credit transfer (compensation)
     */
    private void reverseCreditTransfer(UUID transferId) {
        log.debug("Reversing credit transfer: {}", transferId);

        carbonCreditServiceClient.reverseTransfer(transferId);
    }

    /**
     * Generate certificate
     */
    private CertificateResult generateCertificate(Transaction transaction,
            CreditTransferResult transfer) {
        log.debug("Generating certificate for transaction: {}", transaction.getTransactionId());

        return certificateServiceClient.generateCertificate(
                GenerateCertificateRequest.builder()
                        .transactionId(transaction.getTransactionId())
                        .buyerId(transaction.getBuyerId())
                        .creditAmount(transaction.getCreditAmountTons())
                        .transferId(transfer.getTransferId())
                        .build());
    }

    /**
     * Void certificate (compensation)
     */
    private void voidCertificate(UUID certificateId) {
        log.debug("Voiding certificate: {}", certificateId);

        certificateServiceClient.voidCertificate(certificateId);
    }

    /**
     * Schedule settlement
     */
    private SettlementScheduleResult scheduleSettlement(Transaction transaction,
            EscrowAccount escrow) {
        return escrowService.scheduleSettlement(transaction, escrow);
    }

    /**
     * Cancel settlement (compensation)
     */
    private void cancelSettlement(UUID settlementId) {
        escrowService.cancelSettlement(settlementId);
    }

    /**
     * Send transaction notifications
     */
    private void sendTransactionNotifications(Transaction transaction,
            CertificateResult certificate) {
        notificationServiceClient.sendTransactionNotification(
                TransactionNotificationRequest.builder()
                        .transactionId(transaction.getTransactionId())
                        .buyerId(transaction.getBuyerId())
                        .sellerId(transaction.getSellerId())
                        .certificateUrl(certificate.getCertificateUrl())
                        .build());
    }

    /**
     * Record saga step execution
     */
    private void recordSagaStep(Transaction transaction, String sagaId,
            String stepName, int order, SagaStep.StepStatus status) {
        SagaStep step = SagaStep.builder()
                .transaction(transaction)
                .sagaId(sagaId)
                .stepName(stepName)
                .stepOrder(order)
                .status(status)
                .stepType(SagaStep.StepType.ACTION)
                .serviceName("TransactionService")
                .methodName(stepName.toLowerCase())
                .build();

        sagaStepRepository.save(step);
    }

    /**
     * Record saga failure
     */
    private void recordSagaFailure(Transaction transaction, String sagaId, String errorMessage) {
        List<SagaStep> steps = sagaStepRepository.findBySagaIdOrderByStepOrder(sagaId);

        // Mark incomplete steps as failed
        steps.stream()
                .filter(step -> step.getStatus() == SagaStep.StepStatus.PENDING ||
                        step.getStatus() == SagaStep.StepStatus.IN_PROGRESS)
                .forEach(step -> {
                    step.setStatus(SagaStep.StepStatus.FAILED);
                    step.setErrorMessage(errorMessage);
                    step.setFailedAt(LocalDateTime.now());
                    sagaStepRepository.save(step);
                });

        // Trigger compensation for completed steps
        compensateTransaction(transaction, sagaId);
    }

    /**
     * Execute compensation for failed transaction
     */
    public void compensateTransaction(Transaction transaction, String sagaId) {
        log.info("Starting compensation for transaction: {}", transaction.getTransactionId());

        List<SagaStep> stepsToCompensate = sagaStepRepository
                .findStepsForCompensation(transaction.getTransactionId());

        // Execute compensation in reverse order
        for (SagaStep step : stepsToCompensate) {
            try {
                executeCompensation(step);
                step.setCompensationStatus(SagaStep.CompensationStatus.COMPLETED);
                step.setCompensatedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Compensation failed for step: {}", step.getStepName(), e);
                step.setCompensationStatus(SagaStep.CompensationStatus.FAILED);
                step.setCompensationError(e.getMessage());
            }
            sagaStepRepository.save(step);
        }

        // Update transaction status
        transaction.setStatus(Transaction.TransactionStatus.REFUNDED);
        transactionRepository.save(transaction);
    }

    /**
     * Execute compensation for a specific step
     */
    private void executeCompensation(SagaStep step) {
        switch (step.getStepName()) {
            case "PROCESS_PAYMENT":
                // Refund payment
                if (step.getTransaction().getPaymentId() != null) {
                    refundPayment(step.getTransaction().getPaymentId());
                }
                break;
            case "LOCK_CREDITS":
                // Unlock credits
                if (step.getMetadata() != null) {
                    unlockCreditsInInventory(step.getMetadata());
                }
                break;
            case "TRANSFER_CREDITS":
                // Reverse credit transfer
                // Implementation would extract transfer ID from step metadata
                break;
            case "GENERATE_CERTIFICATE":
                // Void certificate
                if (step.getTransaction().getCertificateId() != null) {
                    voidCertificate(step.getTransaction().getCertificateId());
                }
                break;
            default:
                log.debug("No compensation needed for step: {}", step.getStepName());
        }
    }
}
