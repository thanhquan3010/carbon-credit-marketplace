package com.carbonmarketplace.carboncreditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for carbon calculation results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonCalculationResult {
    
    private BigDecimal totalDistanceKm;
    private BigDecimal co2SavedKg;
    private BigDecimal creditAmountTons;
    private String methodology;
    private LocalDateTime calculationDate;
    private Integer tripCount;
    private CalculationDetails details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationDetails {
        private BigDecimal iceEmissionFactor;
        private BigDecimal evEmissionFactor;
        private BigDecimal netReductionFactor;
        private BigDecimal averageTripDistance;
        private BigDecimal dataQualityScore;
        private String calculationMethod;
        private String verificationStandard;
    }
}
