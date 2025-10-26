package com.carbonmarketplace.marketplaceservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating a listing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingUpdateRequest {
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @DecimalMin(value = "1000000", message = "Minimum price is 1,000,000 VND per ton")
    private BigDecimal pricePerTonVnd;
}
