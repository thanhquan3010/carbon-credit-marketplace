package com.carbonmarketplace.carboncreditservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for transferring carbon credits between users
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferCreditsRequest {
    
    @NotNull(message = "Recipient user ID is required")
    private UUID toUserId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amountTons;
    
    private String description;
    
    private String notes;
}
