package com.carbonmarketplace.analyticsservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortAnalysisResponse {
    
    private String cohortType;
    private LocalDate startDate;
    private Integer periods;
    private List<Cohort> cohorts;
    private CohortSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cohort {
        private String cohortName;
        private LocalDate cohortDate;
        private Integer initialSize;
        private Map<Integer, CohortPeriodData> periodData;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortPeriodData {
        private Integer period;
        private Integer activeUsers;
        private BigDecimal retentionRate;
        private BigDecimal revenue;
        private BigDecimal averageOrderValue;
        private Integer transactions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortSummary {
        private BigDecimal averageRetention;
        private BigDecimal averageLifetimeValue;
        private Integer bestPerformingCohort;
        private Integer worstPerformingCohort;
        private List<String> insights;
    }
}
