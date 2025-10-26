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
public class EnvironmentalImpactResponse {
    
    private ImpactSummary summary;
    private List<ImpactMetric> metrics;
    private Map<String, Double> impactByCategory;
    private List<ImpactTrend> trends;
    private EquivalentImpact equivalents;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpactSummary {
        private Double totalCO2Offset;
        private Double totalDistanceTracked;
        private Long totalTrips;
        private Double averageCO2PerTrip;
        private Long totalCreditsIssued;
        private Long verifiedCredits;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpactMetric {
        private String metricName;
        private Double value;
        private String unit;
        private BigDecimal changePercentage;
        private String trend;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpactTrend {
        private LocalDateTime date;
        private Double co2Offset;
        private Double distance;
        private Long credits;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquivalentImpact {
        private Long treesPlanted;
        private Long carsOffRoad;
        private Double gallonsSaved;
        private Long homesEnergy;
    }
}
