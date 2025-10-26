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
public class DashboardResponse {
    
    // Executive Summary
    private ExecutiveSummary executiveSummary;
    
    // Key Metrics
    private KeyMetrics keyMetrics;
    
    // Charts Data
    private ChartData chartData;
    
    // Period Information
    private PeriodInfo periodInfo;
    
    // Last Updated
    private LocalDateTime lastUpdated;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutiveSummary {
        private BigDecimal totalRevenue;
        private BigDecimal revenueGrowth;
        private Long totalUsers;
        private BigDecimal userGrowth;
        private Long totalTransactions;
        private BigDecimal transactionGrowth;
        private Double totalCO2Offset;
        private BigDecimal co2Growth;
        private BigDecimal platformHealth;
        private String healthStatus; // EXCELLENT, GOOD, FAIR, POOR
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyMetrics {
        private BigDecimal gmv;
        private BigDecimal averageOrderValue;
        private BigDecimal conversionRate;
        private BigDecimal customerAcquisitionCost;
        private BigDecimal lifetimeValue;
        private BigDecimal churnRate;
        private BigDecimal retentionRate;
        private BigDecimal netPromoterScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private List<TimeSeriesData> revenueTimeSeries;
        private List<TimeSeriesData> userGrowthTimeSeries;
        private List<TimeSeriesData> transactionTimeSeries;
        private List<TimeSeriesData> co2OffsetTimeSeries;
        private Map<String, BigDecimal> userTypeDistribution;
        private Map<String, BigDecimal> revenueByCategory;
        private Map<String, Long> topPerformingRegions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesData {
        private LocalDateTime timestamp;
        private BigDecimal value;
        private String label;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodInfo {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String periodType;
        private Integer totalDays;
    }
}
