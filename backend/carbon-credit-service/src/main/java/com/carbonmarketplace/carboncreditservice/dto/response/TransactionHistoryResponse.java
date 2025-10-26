package com.carbonmarketplace.carboncreditservice.dto.response;

import com.carbonmarketplace.carboncreditservice.entity.CreditTransaction.TransactionStatus;
import com.carbonmarketplace.carboncreditservice.entity.CreditTransaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for credit transaction history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryResponse {
    
    private UUID transactionId;
    private UUID creditId;
    private String creditSerialNumber;
    private TransactionType transactionType;
    private UUID fromUserId;
    private String fromUserName;
    private UUID toUserId;
    private String toUserName;
    private BigDecimal amountTons;
    private BigDecimal unitPriceVnd;
    private BigDecimal totalValueVnd;
    private BigDecimal platformFeeVnd;
    private BigDecimal netAmountVnd;
    private TransactionStatus status;
    private String description;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}
