package com.carbonmarketplace.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for price recommendation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRecommendationResponse {
    
    private BigDecimal recommendedPricePerTon;
    private BigDecimal minPricePerTon;
    private BigDecimal maxPricePerTon;
    private BigDecimal avgPricePerTon;
    private BigDecimal medianPricePerTon;
    private String confidenceLevel;
    private Double confidenceScore;
    private String demandIndicator;
    private Integer dataPointsUsed;
    private LocalDateTime lastUpdated;
    private List<MarketInsight> insights;
    private PriceRange priceRange;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketInsight {
        private String type;
        private String message;
        private String impact;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRange {
        private BigDecimal percentile25;
        private BigDecimal percentile50;
        private BigDecimal percentile75;
        private String trend;
        private Double volatility;
    }
}
