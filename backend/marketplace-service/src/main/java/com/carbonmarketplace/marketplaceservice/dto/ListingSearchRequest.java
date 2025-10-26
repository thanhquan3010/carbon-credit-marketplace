package com.carbonmarketplace.marketplaceservice.dto;

import com.carbonmarketplace.marketplaceservice.entity.Listing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for listing search request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingSearchRequest {
    
    private String searchText;
    private List<String> regions;
    private BigDecimal minPricePerTon;
    private BigDecimal maxPricePerTon;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Listing.ListingType listingType;
    private String verificationStatus;
    private List<String> cvaOrganizations;
    private Integer vintageYear;
    private Integer minVintageYear;
    private Integer maxVintageYear;
    private Double minSellerRating;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
    private Boolean activeOnly = true;
    
    // Geolocation search
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    
    public void setDefaults() {
        if (sortBy == null) sortBy = "createdAt";
        if (sortDirection == null) sortDirection = "DESC";
        if (activeOnly == null) activeOnly = true;
    }
}
