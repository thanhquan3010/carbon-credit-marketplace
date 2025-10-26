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
public class KPIResponse {
    
    private String category;
    private String periodType;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private List<KPIMetric> kpis;
    private KPISummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KPIMetric {
        private String kpiType;
        private String kpiName;
        private BigDecimal currentValue;
        private BigDecimal previousValue;
        private BigDecimal changePercentage;
        private String trend; // UP, DOWN, STABLE
        private BigDecimal targetValue;
        private BigDecimal achievementPercentage;
        private String unit;
        private String status; // ON_TRACK, AT_RISK, OFF_TRACK
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KPISummary {
        private Integer totalKPIs;
        private Integer onTrack;
        private Integer atRisk;
        private Integer offTrack;
        private BigDecimal overallPerformance;
        private List<String> recommendations;
    }
}
