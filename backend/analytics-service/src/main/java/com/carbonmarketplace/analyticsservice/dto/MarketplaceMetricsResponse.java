package com.carbonmarketplace.analyticsservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceMetricsResponse {
    
    private MarketplaceSummary summary;
    private ListingMetrics listings;
    private AuctionMetrics auctions;
    private PricingMetrics pricing;
    private List<TopPerformer> topSellers;
    private List<TopPerformer> topBuyers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplaceSummary {
        private Long totalListings;
        private Long activeListings;
        private Long completedTransactions;
        private BigDecimal totalVolume;
        private BigDecimal averageTransactionValue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingMetrics {
        private Long newListings;
        private Long expiredListings;
        private BigDecimal averageListingDuration;
        private Map<String, Long> listingsByCategory;
        private BigDecimal conversionRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionMetrics {
        private Long totalAuctions;
        private Long activeAuctions;
        private BigDecimal averageBidCount;
        private BigDecimal averageFinalPrice;
        private BigDecimal bidParticipationRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingMetrics {
        private BigDecimal averagePricePerCredit;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal priceVolatility;
        private List<PriceTrend> priceTrends;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPerformer {
        private Long userId;
        private String userName;
        private BigDecimal totalVolume;
        private Integer transactionCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceTrend {
        private LocalDateTime date;
        private BigDecimal averagePrice;
        private Long volume;
    }
}
