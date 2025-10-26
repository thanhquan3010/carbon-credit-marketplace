package com.carbonmarketplace.analyticsservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private Double creditAmount;
    private String transactionType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long listingId;
    private Long buyerId;
    private Long sellerId;
    private String paymentMethod;
    private String transactionHash;
    private BigDecimal platformFee;
    private BigDecimal netAmount;
    private String currency;
    private String description;
}
