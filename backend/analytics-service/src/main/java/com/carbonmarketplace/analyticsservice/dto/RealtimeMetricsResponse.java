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
public class RealtimeMetricsResponse {
    
    private CurrentMetrics currentMetrics;
    private List<RecentActivity> recentActivities;
    private SystemStatus systemStatus;
    private LocalDateTime timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentMetrics {
        private Long activeUsers;
        private Long ongoingTransactions;
        private BigDecimal todayRevenue;
        private Double todayCO2Offset;
        private Long activeListings;
        private Long activeAuctions;
        private BigDecimal averageResponseTime;
        private BigDecimal systemLoad;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String activityType;
        private String description;
        private LocalDateTime timestamp;
        private String userId;
        private BigDecimal value;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStatus {
        private String overallStatus;
        private BigDecimal apiLatency;
        private BigDecimal databaseLatency;
        private BigDecimal cacheHitRate;
        private Long errorCount;
        private BigDecimal uptime;
    }
}
