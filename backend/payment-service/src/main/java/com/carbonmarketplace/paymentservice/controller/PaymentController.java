package com.carbonmarketplace.paymentservice.controller;

import com.carbonmarketplace.paymentservice.dto.PaymentRequest;
import com.carbonmarketplace.paymentservice.dto.PaymentResponse;
import com.carbonmarketplace.paymentservice.dto.RefundRequest;
import com.carbonmarketplace.paymentservice.entity.Payment;
import com.carbonmarketplace.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create a new payment", description = "Initiate a payment for a transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Payment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Payment already exists for this transaction"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('BUYER', 'CORPORATE_BUYER')")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        try {
            log.info("Creating payment for transaction: {}", request.getTransactionId());
            PaymentResponse response = paymentService.createPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating payment", e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }

    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    public ResponseEntity<Payment> getPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        try {
            Payment payment = paymentService.getPayment(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error fetching payment: {}", paymentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get payment by transaction ID", description = "Retrieve payment details by transaction ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    public ResponseEntity<Payment> getPaymentByTransaction(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {
        try {
            Payment payment = paymentService.getPaymentByTransaction(transactionId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error fetching payment for transaction: {}", transactionId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Query payment status", description = "Query current payment status from gateway")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "500", description = "Error querying payment gateway")
    })
    @GetMapping("/{paymentId}/status")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    public ResponseEntity<Payment> queryPaymentStatus(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId) {
        try {
            Payment payment = paymentService.queryPaymentStatus(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error querying payment status: {}", paymentId, e);
            throw new RuntimeException("Failed to query payment status: " + e.getMessage());
        }
    }

    @Operation(summary = "Process refund", description = "Initiate a refund for a completed payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refund processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid refund request"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "500", description = "Error processing refund")
    })
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Payment> processRefund(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request) {
        try {
            // Set payment ID in request
            request.setPaymentId(paymentId);
            log.info("Processing refund for payment: {}", paymentId);
            Payment payment = paymentService.processRefund(request);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error processing refund for payment: {}", paymentId, e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage());
        }
    }

    @Operation(summary = "Get my payments", description = "Retrieve paginated list of payments for current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    @GetMapping("/my-payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Payment>> getMyPayments(
            @Parameter(description = "User ID") @RequestParam UUID userId,
            @Parameter(description = "Payment status filter") @RequestParam(required = false) Payment.PaymentStatus status,
            @Parameter(description = "Date range start") @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "Date range end") @RequestParam(required = false) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {
        // In production, get userId from security context instead of request param
        // This is simplified for demonstration
        return ResponseEntity.ok(Page.empty(pageable));
    }

    @Operation(summary = "Get payment statistics", description = "Retrieve payment statistics (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(
            @Parameter(description = "Date range start") @RequestParam LocalDateTime startDate,
            @Parameter(description = "Date range end") @RequestParam LocalDateTime endDate) {
        // This would return various statistics about payments
        return ResponseEntity.ok(Map.of(
            "totalPayments", 0,
            "completedPayments", 0,
            "failedPayments", 0,
            "totalRevenue", 0,
            "platformFees", 0
        ));
    }
}
