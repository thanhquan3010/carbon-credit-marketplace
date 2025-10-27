package com.carbonmarketplace.analyticsservice.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_analytics", indexes = {
        @Index(name = "idx_user_analytics_user_id", columnList = "user_id"),
        @Index(name = "idx_user_analytics_period", columnList = "period_start, period_end")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", length = 50) // EV_OWNER, BUYER, CVA_AUDITOR
    private String userType;

    // Activity metrics
    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "total_credits_earned")
    private Double totalCreditsEarned;

    @Column(name = "total_credits_purchased")
    private Double totalCreditsPurchased;

    @Column(name = "total_credits_sold")
    private Double totalCreditsSold;

    @Column(name = "total_revenue", precision = 19, scale = 4)
    private BigDecimal totalRevenue;

    @Column(name = "total_spending", precision = 19, scale = 4)
    private BigDecimal totalSpending;

    // CO2 metrics
    @Column(name = "total_co2_offset")
    private Double totalCo2Offset;

    @Column(name = "total_distance_tracked")
    private Double totalDistanceTracked;

    // Engagement metrics
    @Column(name = "last_active_date")
    private LocalDateTime lastActiveDate;

    @Column(name = "days_active")
    private Integer daysActive;

    @Column(name = "engagement_score", precision = 5, scale = 2)
    private BigDecimal engagementScore;

    @Column(name = "retention_status", length = 50) // ACTIVE, CHURNED, AT_RISK
    private String retentionStatus;

    // Period information
    @Column(name = "period_type", length = 50) // DAILY, WEEKLY, MONTHLY, ALL_TIME
    private String periodType;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    // Additional analytics
    @Column(name = "avg_transaction_value", precision = 19, scale = 4)
    private BigDecimal avgTransactionValue;

    @Column(name = "lifetime_value", precision = 19, scale = 4)
    private BigDecimal lifetimeValue;

    @Column(name = "churn_probability", precision = 5, scale = 4)
    private BigDecimal churnProbability;
}
