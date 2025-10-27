package com.carbonmarketplace.transactionservice.controller;

import com.carbonmarketplace.transactionservice.config.MetricsConfig;
import com.carbonmarketplace.transactionservice.dto.*;
import com.carbonmarketplace.transactionservice.entity.*;
import com.carbonmarketplace.transactionservice.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Controller
 * 
 * REST API endpoints for transaction management.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Transaction Management", description = "APIs for managing carbon credit transactions")
public class TransactionController {

    private final TransactionSagaService transactionSagaService;
    private final SettlementService settlementService;
    private final RefundService refundService;
    private final EscrowService escrowService;
    private final MetricsConfig.MetricsCollector metricsCollector;

    @Operation(summary = "Create a new purchase transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Duplicate transaction")
    })
    @PostMapping("/purchase")
    @PreAuthorize("hasRole('USER') or hasRole('CORPORATE')")
    @Timed(value = "transaction.creation.time", description = "Time taken to create a transaction")
    public ResponseEntity<TransactionResult> createPurchaseTransaction(
            @Valid @RequestBody PurchaseRequest request) {
        log.info("Creating purchase transaction for listing: {}", request.getListingId());

        TransactionResult result = transactionSagaService.executePurchase(request);

        if (result.isSuccess()) {
            // Record metrics
            metricsCollector.recordTransactionCreated("success");
            // Note: Uncomment and adjust when Transaction entity has amount field
            // if (result.getTransaction() != null && result.getTransaction().getAmount() !=
            // null) {
            // metricsCollector.recordTransactionAmount(
            // result.getTransaction().getAmount().doubleValue(),
            // "purchase"
            // );
            // }
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            metricsCollector.recordTransactionCreated("failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @Operation(summary = "Get transaction by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Transaction> getTransaction(
            @PathVariable UUID transactionId) {
        log.debug("Fetching transaction: {}", transactionId);

        // Implementation would fetch from repository
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get user transactions")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<Transaction>> getUserTransactions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching transactions for user: {}", userId);

        // Implementation would fetch from repository
        return ResponseEntity.ok().build();
    }

    // Settlement Endpoints

    @Operation(summary = "Get settlement batch by ID")
    @GetMapping("/settlements/{batchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettlementBatch> getSettlementBatch(
            @PathVariable UUID batchId) {
        log.debug("Fetching settlement batch: {}", batchId);

        // Implementation would fetch from repository
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Approve settlement batch")
    @PostMapping("/settlements/{batchId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "settlement.approval.time", description = "Time taken to approve settlement")
    public ResponseEntity<Void> approveSettlementBatch(
            @PathVariable UUID batchId,
            @RequestParam UUID approvedBy,
            @RequestParam(required = false) String notes) {
        log.info("Approving settlement batch: {}", batchId);

        settlementService.approveSettlementBatch(batchId, approvedBy, notes);
        metricsCollector.recordSettlementProcessed("approved");

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get settlement statistics")
    @GetMapping("/settlements/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettlementService.SettlementStatistics> getSettlementStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Fetching settlement statistics from {} to {}", startDate, endDate);

        SettlementService.SettlementStatistics stats = settlementService.getSettlementStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get pending settlements")
    @GetMapping("/settlements/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SettlementBatch>> getPendingSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching pending settlements");

        Page<SettlementBatch> settlements = settlementService.getPendingSettlements(page, size);
        return ResponseEntity.ok(settlements);
    }

    // Refund Endpoints

    @Operation(summary = "Request a refund")
    @PostMapping("/refunds")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Timed(value = "refund.request.time", description = "Time taken to process refund request")
    public ResponseEntity<RefundRequest> requestRefund(
            @Valid @RequestBody RefundService.RefundRequestDto request) {
        log.info("Creating refund request for transaction: {}", request.getTransactionId());

        RefundRequest refund = refundService.createRefundRequest(request);
        metricsCollector.recordRefundRequest("created");

        return ResponseEntity.status(HttpStatus.CREATED).body(refund);
    }

    @Operation(summary = "Approve refund request")
    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveRefund(
            @PathVariable UUID refundId,
            @RequestParam UUID approvedBy,
            @RequestParam(required = false) String notes) {
        log.info("Approving refund: {}", refundId);

        refundService.approveRefund(refundId, approvedBy, notes);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reject refund request")
    @PostMapping("/refunds/{refundId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectRefund(
            @PathVariable UUID refundId,
            @RequestParam UUID rejectedBy,
            @RequestParam String reason) {
        log.info("Rejecting refund: {}", refundId);

        refundService.rejectRefund(refundId, rejectedBy, reason);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get pending approval refunds")
    @GetMapping("/refunds/pending-approval")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RefundRequest>> getPendingApprovalRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching pending approval refunds");

        Page<RefundRequest> refunds = refundService.getPendingApprovalRefunds(page, size);
        return ResponseEntity.ok(refunds);
    }

    @Operation(summary = "Get refund statistics")
    @GetMapping("/refunds/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefundService.RefundStatistics> getRefundStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.debug("Fetching refund statistics from {} to {}", startDate, endDate);

        RefundService.RefundStatistics stats = refundService.getRefundStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // Escrow Endpoints

    @Operation(summary = "Get escrow statistics")
    @GetMapping("/escrow/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EscrowService.EscrowStatistics> getEscrowStatistics() {
        log.debug("Fetching escrow statistics");

        EscrowService.EscrowStatistics stats = escrowService.getEscrowStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Process early release")
    @PostMapping("/escrow/{escrowAccountId}/early-release")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "escrow.early.release.time", description = "Time taken to process early release")
    public ResponseEntity<Void> processEarlyRelease(
            @PathVariable UUID escrowAccountId,
            @RequestParam UUID approvedBy,
            @RequestParam String reason) {
        log.info("Processing early release for escrow: {}", escrowAccountId);

        escrowService.earlyRelease(escrowAccountId, approvedBy, reason);
        metricsCollector.recordEscrowOperation("early_release");

        return ResponseEntity.ok().build();
    }

    // Health Check
    @Operation(summary = "Health check")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is running");
    }
}
