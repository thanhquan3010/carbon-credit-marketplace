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
public class ForecastResponse {
    
    private String metricType;
    private Integer forecastDays;
    private List<ForecastPoint> forecast;
    private ForecastAccuracy accuracy;
    private String model;
    private BigDecimal confidence;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastPoint {
        private LocalDateTime date;
        private BigDecimal predictedValue;
        private BigDecimal lowerBound;
        private BigDecimal upperBound;
        private BigDecimal probability;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastAccuracy {
        private BigDecimal meanAbsoluteError;
        private BigDecimal meanSquaredError;
        private BigDecimal rootMeanSquaredError;
        private BigDecimal meanAbsolutePercentageError;
        private BigDecimal r2Score;
    }
}
