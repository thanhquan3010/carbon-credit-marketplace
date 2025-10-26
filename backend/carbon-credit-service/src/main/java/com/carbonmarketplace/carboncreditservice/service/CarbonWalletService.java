package com.carbonmarketplace.carboncreditservice.service;

import com.carbonmarketplace.carboncreditservice.dto.request.RetireCreditRequest;
import com.carbonmarketplace.carboncreditservice.dto.request.TransferCreditsRequest;
import com.carbonmarketplace.carboncreditservice.dto.response.TransactionHistoryResponse;
import com.carbonmarketplace.carboncreditservice.dto.response.WalletResponse;
import com.carbonmarketplace.carboncreditservice.entity.*;
import com.carbonmarketplace.carboncreditservice.exception.InsufficientBalanceException;
import com.carbonmarketplace.carboncreditservice.exception.WalletNotFoundException;
import com.carbonmarketplace.carboncreditservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing carbon credit wallets and balance tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarbonWalletService {
    
    private final CarbonWalletRepository walletRepository;
    private final CarbonCreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;
    
    /**
     * Get or create a wallet for a user
     */
    @Transactional
    public CarbonWallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new wallet for user: {}", userId);
                    CarbonWallet wallet = CarbonWallet.builder()
                            .userId(userId)
                            .status(CarbonWallet.WalletStatus.ACTIVE)
                            .build();
                    return walletRepository.save(wallet);
                });
    }
    
    /**
     * Get wallet details for a user
     */
    @Transactional(readOnly = true)
    public WalletResponse getWalletDetails(UUID userId) {
        CarbonWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        
        return mapToWalletResponse(wallet);
    }
    
    /**
     * Add verified credits to wallet
     */
    @Transactional
    public void addVerifiedCredits(UUID userId, BigDecimal amountTons, UUID verificationId) {
        log.info("Adding {} tons of verified credits to user {} wallet", amountTons, userId);
        
        CarbonWallet wallet = getOrCreateWallet(userId);
        
        if (!wallet.isActive()) {
            throw new IllegalStateException("Cannot add credits to inactive wallet");
        }
        
        wallet.addCredits(amountTons);
        walletRepository.save(wallet);
        
        // Create issuance transaction record
        CreditTransaction transaction = CreditTransaction.builder()
                .transactionType(CreditTransaction.TransactionType.ISSUANCE)
                .toUserId(userId)
                .amountTons(amountTons)
                .status(CreditTransaction.TransactionStatus.COMPLETED)
                .description("Credits issued from verification: " + verificationId)
                .completedAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);
        
        log.info("Successfully added {} tons to wallet. New balance: {} tons",
                amountTons, wallet.getAvailableBalanceTons());
    }
    
    /**
     * Transfer credits between users
     */
    @Transactional
    public TransactionHistoryResponse transferCredits(UUID fromUserId, TransferCreditsRequest request) {
        log.info("Transferring {} tons from user {} to user {}",
                request.getAmountTons(), fromUserId, request.getToUserId());
        
        if (fromUserId.equals(request.getToUserId())) {
            throw new IllegalArgumentException("Cannot transfer credits to yourself");
        }
        
        // Get sender wallet
        CarbonWallet senderWallet = walletRepository.findByUserId(fromUserId)
                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));
        
        if (!senderWallet.isActive()) {
            throw new IllegalStateException("Sender wallet is not active");
        }
        
        // Check balance
        if (senderWallet.getAvailableBalanceTons().compareTo(request.getAmountTons()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + senderWallet.getAvailableBalanceTons()
            );
        }
        
        // Get receiver wallet
        CarbonWallet receiverWallet = getOrCreateWallet(request.getToUserId());
        
        if (!receiverWallet.isActive()) {
            throw new IllegalStateException("Receiver wallet is not active");
        }
        
        // Lock credits in sender wallet
        senderWallet.lockCredits(request.getAmountTons());
        
        // Create transaction record
        CreditTransaction transaction = CreditTransaction.builder()
                .transactionType(CreditTransaction.TransactionType.TRANSFER)
                .fromUserId(fromUserId)
                .toUserId(request.getToUserId())
                .amountTons(request.getAmountTons())
                .status(CreditTransaction.TransactionStatus.PROCESSING)
                .description(request.getDescription())
                .notes(request.getNotes())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        try {
            // Transfer credits
            List<CarbonCredit> creditsToTransfer = selectCreditsForTransfer(
                    fromUserId, 
                    request.getAmountTons()
            );
            
            for (CarbonCredit credit : creditsToTransfer) {
                credit.transferOwnership(request.getToUserId());
                creditRepository.save(credit);
            }
            
            // Update wallets
            senderWallet.completeSale(request.getAmountTons());
            receiverWallet.addCredits(request.getAmountTons());
            
            walletRepository.save(senderWallet);
            walletRepository.save(receiverWallet);
            
            // Complete transaction
            transaction.complete();
            transaction = transactionRepository.save(transaction);
            
            log.info("Transfer completed successfully. Transaction ID: {}", transaction.getTransactionId());
            
        } catch (Exception e) {
            // Rollback on error
            log.error("Transfer failed: {}", e.getMessage());
            senderWallet.releaseLockedCredits(request.getAmountTons());
            walletRepository.save(senderWallet);
            
            transaction.cancel("Transfer failed: " + e.getMessage());
            transaction = transactionRepository.save(transaction);
            
            throw new RuntimeException("Transfer failed", e);
        }
        
        return mapToTransactionResponse(transaction);
    }
    
    /**
     * Retire credits
     */
    @Transactional
    public TransactionHistoryResponse retireCredits(UUID userId, RetireCreditRequest request) {
        log.info("Retiring {} tons of credits for user {}", request.getAmountTons(), userId);
        
        CarbonWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        
        if (!wallet.isActive()) {
            throw new IllegalStateException("Wallet is not active");
        }
        
        if (wallet.getAvailableBalanceTons().compareTo(request.getAmountTons()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for retirement. Available: " + wallet.getAvailableBalanceTons()
            );
        }
        
        // Select credits for retirement
        List<CarbonCredit> creditsToRetire;
        if (request.getCreditIds() != null && !request.getCreditIds().isEmpty()) {
            creditsToRetire = creditRepository.findByCreditIdIn(request.getCreditIds());
            
            // Validate ownership
            for (CarbonCredit credit : creditsToRetire) {
                if (!credit.getOwnerId().equals(userId)) {
                    throw new IllegalArgumentException("Credit " + credit.getCreditId() + " not owned by user");
                }
            }
        } else {
            creditsToRetire = selectCreditsForRetirement(userId, request.getAmountTons());
        }
        
        // Create transaction
        CreditTransaction transaction = CreditTransaction.builder()
                .transactionType(CreditTransaction.TransactionType.RETIREMENT)
                .fromUserId(userId)
                .amountTons(request.getAmountTons())
                .status(CreditTransaction.TransactionStatus.PROCESSING)
                .description("Retirement: " + request.getRetirementReason())
                .notes("Beneficiary: " + request.getBeneficiary())
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        try {
            // Retire credits
            BigDecimal totalRetired = BigDecimal.ZERO;
            for (CarbonCredit credit : creditsToRetire) {
                credit.retire(request.getRetirementReason());
                creditRepository.save(credit);
                totalRetired = totalRetired.add(credit.getAmountTons());
            }
            
            // Update wallet
            wallet.retireCredits(totalRetired);
            walletRepository.save(wallet);
            
            // Complete transaction
            transaction.complete();
            transaction = transactionRepository.save(transaction);
            
            log.info("Successfully retired {} tons of credits", totalRetired);
            
        } catch (Exception e) {
            log.error("Retirement failed: {}", e.getMessage());
            transaction.cancel("Retirement failed: " + e.getMessage());
            transactionRepository.save(transaction);
            throw new RuntimeException("Retirement failed", e);
        }
        
        return mapToTransactionResponse(transaction);
    }
    
    /**
     * Get transaction history for a user
     */
    @Transactional(readOnly = true)
    public Page<TransactionHistoryResponse> getTransactionHistory(UUID userId, Pageable pageable) {
        Page<CreditTransaction> transactions = transactionRepository.findUserTransactions(userId, pageable);
        return transactions.map(this::mapToTransactionResponse);
    }
    
    /**
     * Lock credits for a marketplace transaction
     */
    @Transactional
    public void lockCreditsForSale(UUID userId, BigDecimal amountTons) {
        log.info("Locking {} tons for sale from user {}", amountTons, userId);
        
        CarbonWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        
        if (!wallet.isActive()) {
            throw new IllegalStateException("Wallet is not active");
        }
        
        wallet.lockCredits(amountTons);
        walletRepository.save(wallet);
    }
    
    /**
     * Release locked credits (e.g., when sale is cancelled)
     */
    @Transactional
    public void releaseLockedCredits(UUID userId, BigDecimal amountTons) {
        log.info("Releasing {} tons of locked credits for user {}", amountTons, userId);
        
        CarbonWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        
        wallet.releaseLockedCredits(amountTons);
        walletRepository.save(wallet);
    }
    
    /**
     * Complete a sale transaction
     */
    @Transactional
    public void completeSale(UUID sellerId, UUID buyerId, BigDecimal amountTons, BigDecimal priceVnd) {
        log.info("Completing sale: {} tons from {} to {} for {} VND",
                amountTons, sellerId, buyerId, priceVnd);
        
        // Update seller wallet
        CarbonWallet sellerWallet = walletRepository.findByUserId(sellerId)
                .orElseThrow(() -> new WalletNotFoundException("Seller wallet not found"));
        
        sellerWallet.completeSale(amountTons);
        walletRepository.save(sellerWallet);
        
        // Update buyer wallet
        CarbonWallet buyerWallet = getOrCreateWallet(buyerId);
        buyerWallet.addCredits(amountTons);
        walletRepository.save(buyerWallet);
        
        // Transfer ownership of credits
        List<CarbonCredit> creditsToTransfer = selectCreditsForTransfer(sellerId, amountTons);
        for (CarbonCredit credit : creditsToTransfer) {
            credit.transferOwnership(buyerId);
            credit.setStatus(CarbonCredit.CreditStatus.SOLD);
            creditRepository.save(credit);
        }
        
        // Create transaction record
        CreditTransaction transaction = CreditTransaction.builder()
                .transactionType(CreditTransaction.TransactionType.SALE)
                .fromUserId(sellerId)
                .toUserId(buyerId)
                .amountTons(amountTons)
                .unitPriceVnd(priceVnd.divide(amountTons, 2, BigDecimal.ROUND_HALF_UP))
                .totalValueVnd(priceVnd)
                .status(CreditTransaction.TransactionStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);
    }
    
    /**
     * Select credits for transfer (FIFO)
     */
    private List<CarbonCredit> selectCreditsForTransfer(UUID ownerId, BigDecimal amountNeeded) {
        List<CarbonCredit> availableCredits = creditRepository
                .findByOwnerIdAndStatusAndRetiredAtIsNull(ownerId, CarbonCredit.CreditStatus.ISSUED);
        
        List<CarbonCredit> selected = new ArrayList<>();
        BigDecimal totalSelected = BigDecimal.ZERO;
        
        for (CarbonCredit credit : availableCredits) {
            if (totalSelected.compareTo(amountNeeded) >= 0) {
                break;
            }
            selected.add(credit);
            totalSelected = totalSelected.add(credit.getAmountTons());
        }
        
        if (totalSelected.compareTo(amountNeeded) < 0) {
            throw new InsufficientBalanceException(
                    "Not enough available credits. Need: " + amountNeeded + ", Available: " + totalSelected
            );
        }
        
        return selected;
    }
    
    /**
     * Select credits for retirement (FIFO)
     */
    private List<CarbonCredit> selectCreditsForRetirement(UUID ownerId, BigDecimal amountNeeded) {
        Pageable pageable = Pageable.ofSize(100);
        List<CarbonCredit> availableCredits = creditRepository
                .findAvailableCreditsForRetirement(ownerId, pageable);
        
        List<CarbonCredit> selected = new ArrayList<>();
        BigDecimal totalSelected = BigDecimal.ZERO;
        
        for (CarbonCredit credit : availableCredits) {
            if (totalSelected.compareTo(amountNeeded) >= 0) {
                break;
            }
            selected.add(credit);
            totalSelected = totalSelected.add(credit.getAmountTons());
        }
        
        if (totalSelected.compareTo(amountNeeded) < 0) {
            throw new InsufficientBalanceException(
                    "Not enough credits for retirement. Need: " + amountNeeded + ", Available: " + totalSelected
            );
        }
        
        return selected;
    }
    
    /**
     * Map wallet entity to response DTO
     */
    private WalletResponse mapToWalletResponse(CarbonWallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .availableBalanceTons(wallet.getAvailableBalanceTons())
                .pendingBalanceTons(wallet.getPendingBalanceTons())
                .lockedBalanceTons(wallet.getLockedBalanceTons())
                .totalBalanceTons(wallet.getTotalBalanceTons())
                .totalEarnedTons(wallet.getTotalEarnedTons())
                .totalSoldTons(wallet.getTotalSoldTons())
                .totalRetiredTons(wallet.getTotalRetiredTons())
                .totalTransactions(wallet.getTotalTransactions())
                .totalVerifications(wallet.getTotalVerifications())
                .lastTransactionDate(wallet.getLastTransactionDate())
                .status(wallet.getStatus())
                .frozenReason(wallet.getFrozenReason())
                .frozenAt(wallet.getFrozenAt())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
    
    /**
     * Map transaction entity to response DTO
     */
    private TransactionHistoryResponse mapToTransactionResponse(CreditTransaction transaction) {
        return TransactionHistoryResponse.builder()
                .transactionId(transaction.getTransactionId())
                .transactionType(transaction.getTransactionType())
                .fromUserId(transaction.getFromUserId())
                .toUserId(transaction.getToUserId())
                .amountTons(transaction.getAmountTons())
                .unitPriceVnd(transaction.getUnitPriceVnd())
                .totalValueVnd(transaction.getTotalValueVnd())
                .platformFeeVnd(transaction.getPlatformFeeVnd())
                .netAmountVnd(transaction.getNetAmountVnd())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .cancelledAt(transaction.getCancelledAt())
                .cancellationReason(transaction.getCancellationReason())
                .build();
    }
}
