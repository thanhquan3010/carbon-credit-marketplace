package com.carbonmarketplace.carboncreditservice.dto.response;

import com.carbonmarketplace.carboncreditservice.entity.CarbonWallet.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for carbon wallet details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    
    private UUID walletId;
    private UUID userId;
    private BigDecimal availableBalanceTons;
    private BigDecimal pendingBalanceTons;
    private BigDecimal lockedBalanceTons;
    private BigDecimal totalBalanceTons;
    private BigDecimal totalEarnedTons;
    private BigDecimal totalSoldTons;
    private BigDecimal totalRetiredTons;
    private Integer totalTransactions;
    private Integer totalVerifications;
    private LocalDateTime lastTransactionDate;
    private WalletStatus status;
    private String frozenReason;
    private LocalDateTime frozenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
