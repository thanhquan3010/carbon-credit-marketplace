package com.carbonmarketplace.analyticsservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAnalysisResponse {
    
    private SystemPerformance systemPerformance;
    private List<Bottleneck> bottlenecks;
    private List<Recommendation> recommendations;
    private ResourceUtilization resourceUtilization;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemPerformance {
        private BigDecimal averageResponseTime;
        private BigDecimal p95ResponseTime;
        private BigDecimal p99ResponseTime;
        private Long requestsPerSecond;
        private BigDecimal errorRate;
        private BigDecimal uptime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bottleneck {
        private String component;
        private String issue;
        private String severity; // HIGH, MEDIUM, LOW
        private BigDecimal impact;
        private String recommendation;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String area;
        private String action;
        private String expectedBenefit;
        private String priority;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUtilization {
        private BigDecimal cpuUsage;
        private BigDecimal memoryUsage;
        private BigDecimal diskUsage;
        private BigDecimal networkBandwidth;
        private BigDecimal databaseConnections;
        private BigDecimal cacheHitRatio;
    }
}
