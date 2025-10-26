package com.carbonmarketplace.analyticsservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysisResponse {
    
    private String metricType;
    private String periodType;
    private List<TrendPoint> trendData;
    private TrendStatistics statistics;
    private String trendDirection;
    private BigDecimal predictedNextValue;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private LocalDateTime timestamp;
        private BigDecimal value;
        private BigDecimal movingAverage;
        private BigDecimal upperBound;
        private BigDecimal lowerBound;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendStatistics {
        private BigDecimal mean;
        private BigDecimal median;
        private BigDecimal standardDeviation;
        private BigDecimal min;
        private BigDecimal max;
        private BigDecimal growthRate;
        private BigDecimal correlation;
    }
}
