package com.carbonmarketplace.marketplaceservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for placing a bid on an auction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {
    
    @NotNull(message = "Listing ID is required")
    private UUID listingId;
    
    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0", message = "Bid amount must be positive")
    private BigDecimal bidAmountPerTon;
    
    @DecimalMin(value = "0", message = "Max auto-bid amount must be positive")
    private BigDecimal maxAutoBidAmount;
    
    private String bidderName;
    
    public void validateAutoBid() {
        if (maxAutoBidAmount != null && maxAutoBidAmount.compareTo(bidAmountPerTon) < 0) {
            throw new IllegalArgumentException("Max auto-bid amount must be greater than or equal to bid amount");
        }
    }
}
