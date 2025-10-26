package com.carbonmarketplace.marketplaceservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for adding items to cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    
    @NotNull(message = "Listing ID is required")
    private UUID listingId;
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.1", message = "Minimum quantity is 0.1 tons")
    @DecimalMax(value = "100", message = "Maximum quantity is 100 tons")
    private BigDecimal quantityTons;
}
