package com.carbonmarketplace.carboncreditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's carbon credit wallet
 */
@Entity
@Table(name = "carbon_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonWallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;
    
    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;
    
    // Wallet Balance
    @Column(name = "available_balance_tons", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal availableBalanceTons = BigDecimal.ZERO;
    
    @Column(name = "pending_balance_tons", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal pendingBalanceTons = BigDecimal.ZERO;
    
    @Column(name = "locked_balance_tons", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal lockedBalanceTons = BigDecimal.ZERO;
    
    @Column(name = "total_earned_tons", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal totalEarnedTons = BigDecimal.ZERO;
    
    @Column(name = "total_sold_tons", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal totalSoldTons = BigDecimal.ZERO;
    
    @Column(name = "total_retired_tons", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal totalRetiredTons = BigDecimal.ZERO;
    
    // Statistics
    @Column(name = "total_transactions", nullable = false)
    @Builder.Default
    private Integer totalTransactions = 0;
    
    @Column(name = "total_verifications", nullable = false)
    @Builder.Default
    private Integer totalVerifications = 0;
    
    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;
    
    // Wallet Status
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;
    
    @Column(name = "frozen_reason", length = 500)
    private String frozenReason;
    
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;
    
    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum WalletStatus {
        ACTIVE,
        FROZEN,
        SUSPENDED,
        CLOSED
    }
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = WalletStatus.ACTIVE;
        }
        initializeBalances();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private void initializeBalances() {
        if (availableBalanceTons == null) {
            availableBalanceTons = BigDecimal.ZERO;
        }
        if (pendingBalanceTons == null) {
            pendingBalanceTons = BigDecimal.ZERO;
        }
        if (lockedBalanceTons == null) {
            lockedBalanceTons = BigDecimal.ZERO;
        }
        if (totalEarnedTons == null) {
            totalEarnedTons = BigDecimal.ZERO;
        }
        if (totalSoldTons == null) {
            totalSoldTons = BigDecimal.ZERO;
        }
        if (totalRetiredTons == null) {
            totalRetiredTons = BigDecimal.ZERO;
        }
        if (totalTransactions == null) {
            totalTransactions = 0;
        }
        if (totalVerifications == null) {
            totalVerifications = 0;
        }
    }
    
    /**
     * Get total balance including available, pending, and locked
     */
    public BigDecimal getTotalBalanceTons() {
        return availableBalanceTons
                .add(pendingBalanceTons)
                .add(lockedBalanceTons);
    }
    
    /**
     * Add credits to available balance
     */
    public void addCredits(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.availableBalanceTons = this.availableBalanceTons.add(amount);
        this.totalEarnedTons = this.totalEarnedTons.add(amount);
        this.totalTransactions++;
        this.lastTransactionDate = LocalDateTime.now();
    }
    
    /**
     * Lock credits for a transaction
     */
    public void lockCredits(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (availableBalanceTons.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.availableBalanceTons = this.availableBalanceTons.subtract(amount);
        this.lockedBalanceTons = this.lockedBalanceTons.add(amount);
    }
    
    /**
     * Release locked credits back to available
     */
    public void releaseLockedCredits(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (lockedBalanceTons.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient locked balance");
        }
        this.lockedBalanceTons = this.lockedBalanceTons.subtract(amount);
        this.availableBalanceTons = this.availableBalanceTons.add(amount);
    }
    
    /**
     * Complete a sale transaction
     */
    public void completeSale(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (lockedBalanceTons.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient locked balance");
        }
        this.lockedBalanceTons = this.lockedBalanceTons.subtract(amount);
        this.totalSoldTons = this.totalSoldTons.add(amount);
        this.totalTransactions++;
        this.lastTransactionDate = LocalDateTime.now();
    }
    
    /**
     * Retire credits
     */
    public void retireCredits(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (availableBalanceTons.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.availableBalanceTons = this.availableBalanceTons.subtract(amount);
        this.totalRetiredTons = this.totalRetiredTons.add(amount);
        this.totalTransactions++;
        this.lastTransactionDate = LocalDateTime.now();
    }
    
    /**
     * Freeze the wallet
     */
    public void freeze(String reason) {
        this.status = WalletStatus.FROZEN;
        this.frozenReason = reason;
        this.frozenAt = LocalDateTime.now();
    }
    
    /**
     * Unfreeze the wallet
     */
    public void unfreeze() {
        this.status = WalletStatus.ACTIVE;
        this.frozenReason = null;
        this.frozenAt = null;
    }
    
    /**
     * Check if wallet is active and operational
     */
    public boolean isActive() {
        return status == WalletStatus.ACTIVE;
    }
}
