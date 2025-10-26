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
public class UserAnalyticsResponse {
    
    private Long userId;
    private String userType;
    private UserMetrics metrics;
    private UserActivity activity;
    private UserValue value;
    private List<UserTransaction> recentTransactions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMetrics {
        private Integer totalTransactions;
        private Double totalCreditsEarned;
        private Double totalCreditsPurchased;
        private BigDecimal totalRevenue;
        private Double totalCo2Offset;
        private Double totalDistanceTracked;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivity {
        private LocalDateTime lastActiveDate;
        private Integer daysActive;
        private BigDecimal engagementScore;
        private String retentionStatus;
        private List<ActivityPeriod> activityHistory;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserValue {
        private BigDecimal lifetimeValue;
        private BigDecimal avgTransactionValue;
        private BigDecimal churnProbability;
        private String valueSegment;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTransaction {
        private Long transactionId;
        private String type;
        private BigDecimal amount;
        private LocalDateTime timestamp;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityPeriod {
        private LocalDateTime date;
        private Integer transactions;
        private BigDecimal value;
    }
}
