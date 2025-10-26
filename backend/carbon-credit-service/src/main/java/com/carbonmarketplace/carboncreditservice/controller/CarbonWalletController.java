package com.carbonmarketplace.carboncreditservice.controller;

import com.carbonmarketplace.carboncreditservice.dto.request.RetireCreditRequest;
import com.carbonmarketplace.carboncreditservice.dto.request.TransferCreditsRequest;
import com.carbonmarketplace.carboncreditservice.dto.response.TransactionHistoryResponse;
import com.carbonmarketplace.carboncreditservice.dto.response.WalletResponse;
import com.carbonmarketplace.carboncreditservice.service.CarbonWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for carbon wallet management
 */
@RestController
@RequestMapping("/api/v1/carbon/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Carbon Wallet", description = "Carbon wallet management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CarbonWalletController {
    
    private final CarbonWalletService walletService;
    
    @Operation(summary = "Get wallet details",
            description = "Get carbon wallet details for the authenticated user")
    @GetMapping("/my-wallet")
    @PreAuthorize("hasAnyRole('EVOWNER', 'BUYER')")
    public ResponseEntity<WalletResponse> getMyWallet(Authentication authentication) {
        UUID userId = getUserIdFromAuth(authentication);
        
        WalletResponse wallet = walletService.getWalletDetails(userId);
        
        return ResponseEntity.ok(wallet);
    }
    
    @Operation(summary = "Get wallet by user ID",
            description = "Get carbon wallet details for a specific user (admin only)")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponse> getUserWallet(@PathVariable UUID userId) {
        WalletResponse wallet = walletService.getWalletDetails(userId);
        
        return ResponseEntity.ok(wallet);
    }
    
    @Operation(summary = "Transfer credits",
            description = "Transfer carbon credits to another user")
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('EVOWNER', 'BUYER')")
    public ResponseEntity<TransactionHistoryResponse> transferCredits(
            Authentication authentication,
            @Valid @RequestBody TransferCreditsRequest request) {
        
        UUID userId = getUserIdFromAuth(authentication);
        
        log.info("User {} transferring {} tons to user {}",
                userId, request.getAmountTons(), request.getToUserId());
        
        TransactionHistoryResponse transaction = walletService.transferCredits(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }
    
    @Operation(summary = "Retire credits",
            description = "Retire carbon credits permanently")
    @PostMapping("/retire")
    @PreAuthorize("hasAnyRole('EVOWNER', 'BUYER')")
    public ResponseEntity<TransactionHistoryResponse> retireCredits(
            Authentication authentication,
            @Valid @RequestBody RetireCreditRequest request) {
        
        UUID userId = getUserIdFromAuth(authentication);
        
        log.info("User {} retiring {} tons of credits",
                userId, request.getAmountTons());
        
        TransactionHistoryResponse transaction = walletService.retireCredits(userId, request);
        
        return ResponseEntity.ok(transaction);
    }
    
    @Operation(summary = "Get transaction history",
            description = "Get carbon credit transaction history")
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('EVOWNER', 'BUYER')")
    public ResponseEntity<Page<TransactionHistoryResponse>> getTransactionHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        UUID userId = getUserIdFromAuth(authentication);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TransactionHistoryResponse> transactions = walletService.getTransactionHistory(userId, pageable);
        
        return ResponseEntity.ok(transactions);
    }
    
    @Operation(summary = "Get user transactions (admin)",
            description = "Get transaction history for a specific user")
    @GetMapping("/user/{userId}/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionHistoryResponse>> getUserTransactions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<TransactionHistoryResponse> transactions = walletService.getTransactionHistory(userId, pageable);
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Extract user ID from authentication
     */
    private UUID getUserIdFromAuth(Authentication authentication) {
        // In a real implementation, this would extract the user ID from the JWT token
        // For now, using a placeholder
        return UUID.fromString(authentication.getName());
    }
}
