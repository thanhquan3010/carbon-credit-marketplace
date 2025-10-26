package com.carbonmarketplace.marketplaceservice.dto;

import com.carbonmarketplace.marketplaceservice.entity.Listing;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new listing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingCreateRequest {
    
    @NotNull(message = "Credit ID is required")
    private UUID creditId;
    
    @NotNull(message = "Listing type is required")
    private Listing.ListingType listingType;
    
    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.1", message = "Minimum listing amount is 0.1 tons")
    @DecimalMax(value = "100", message = "Maximum listing amount is 100 tons")
    private BigDecimal creditAmountTons;
    
    // Fixed price listing fields
    @DecimalMin(value = "1000000", message = "Minimum price is 1,000,000 VND per ton")
    private BigDecimal pricePerTonVnd;
    
    // Auction listing fields
    @DecimalMin(value = "1000000", message = "Minimum starting price is 1,000,000 VND per ton")
    private BigDecimal startingPricePerTonVnd;
    
    private BigDecimal reservePricePerTonVnd;
    
    @Min(value = 1, message = "Minimum auction duration is 1 day")
    @Max(value = 30, message = "Maximum auction duration is 30 days")
    private Integer auctionDurationDays;
    
    @NotBlank(message = "Region is required")
    @Size(max = 100)
    private String region;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    private Integer vintageYear;
    
    public void validate() {
        if (listingType == Listing.ListingType.FIXED) {
            if (pricePerTonVnd == null) {
                throw new IllegalArgumentException("Price per ton is required for fixed price listing");
            }
        } else if (listingType == Listing.ListingType.AUCTION) {
            if (startingPricePerTonVnd == null) {
                throw new IllegalArgumentException("Starting price is required for auction listing");
            }
            if (auctionDurationDays == null) {
                throw new IllegalArgumentException("Auction duration is required");
            }
            if (creditAmountTons.compareTo(BigDecimal.ONE) < 0) {
                throw new IllegalArgumentException("Minimum amount for auction is 1 ton");
            }
            if (reservePricePerTonVnd != null && 
                reservePricePerTonVnd.compareTo(startingPricePerTonVnd) < 0) {
                throw new IllegalArgumentException("Reserve price must be higher than starting price");
            }
        }
    }
}
