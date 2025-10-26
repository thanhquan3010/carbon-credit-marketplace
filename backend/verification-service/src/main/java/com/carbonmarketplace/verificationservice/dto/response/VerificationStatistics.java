package com.carbonmarketplace.verificationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for verification statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationStatistics {

    // Queue Statistics
    private Integer pendingRequests;
    private Integer inReviewRequests;
    private Integer completedToday;
    private Integer completedThisWeek;
    private Integer completedThisMonth;

    // Performance Metrics
    private Double averageReviewTimeHours;
    private Double slaComplianceRate;
    private Integer overdueRequests;
    private Integer highPriorityPending;

    // Auditor Workload
    private Map<String, AuditorWorkload> auditorWorkloads;
    private Integer totalActiveAuditors;

    // Credit Statistics
    private BigDecimal totalCreditsIssuedTons;
    private BigDecimal creditsIssuedTodayTons;
    private BigDecimal creditsIssuedThisMonthTons;
    private Integer totalCreditIssuances;

    // Quality Metrics
    private Double approvalRate;
    private Double rejectionRate;
    private Double adjustmentRate;
    private BigDecimal averageDataQualityScore;

    // Anomaly Statistics
    private Integer totalAnomaliesDetected;
    private Integer unresolvedAnomalies;
    private Map<String, Integer> anomaliesByType;
    private Map<String, Integer> anomaliesBySeverity;

    // Time Range
    private LocalDateTime statisticsGeneratedAt;
    private String reportPeriod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditorWorkload {
        private String auditorId;
        private String auditorName;
        private Integer assignedRequests;
        private Integer completedToday;
        private Integer inProgress;
        private Double averageReviewTimeHours;
        private Double productivityScore;
    }

    /**
     * Calculate overall health score.
     */
    public String getOverallHealthScore() {
        double score = 0;
        double weightTotal = 0;

        // SLA compliance (40% weight)
        if (slaComplianceRate != null) {
            score += slaComplianceRate * 0.4;
            weightTotal += 0.4;
        }

        // Approval rate (30% weight)
        if (approvalRate != null) {
            score += approvalRate * 0.3;
            weightTotal += 0.3;
        }

        // Queue health (30% weight) - inverse of pending/overdue ratio
        if (pendingRequests != null && overdueRequests != null && pendingRequests > 0) {
            double queueHealth = 1.0 - ((double) overdueRequests / pendingRequests);
            score += queueHealth * 0.3;
            weightTotal += 0.3;
        }

        if (weightTotal == 0) {
            return "Unknown";
        }

        double finalScore = (score / weightTotal) * 100;
        
        if (finalScore >= 90) return "Excellent";
        if (finalScore >= 75) return "Good";
        if (finalScore >= 60) return "Fair";
        return "Needs Improvement";
    }
}
