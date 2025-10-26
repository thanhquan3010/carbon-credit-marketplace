package com.carbonmarketplace.carboncreditservice.service;

import com.carbonmarketplace.carboncreditservice.dto.CarbonCalculationResult;
import com.carbonmarketplace.carboncreditservice.dto.request.CalculateCarbonRequest;
import com.carbonmarketplace.carboncreditservice.entity.Trip;
import com.carbonmarketplace.carboncreditservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for calculating carbon credits based on EV trips
 * Implements the core calculation formula: CO2 Saved (kg) = Distance (km) × (0.15 - 0.05)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarbonCalculationService {
    
    // Emission factors (kg CO2 per km)
    private static final BigDecimal ICE_EMISSION_FACTOR = new BigDecimal("0.15");
    private static final BigDecimal EV_EMISSION_FACTOR = new BigDecimal("0.05");
    private static final BigDecimal KG_TO_TONS = new BigDecimal("1000");
    
    // Accuracy tolerance (±2%)
    private static final BigDecimal ACCURACY_TOLERANCE = new BigDecimal("0.02");
    
    private final TripRepository tripRepository;
    
    /**
     * Calculate carbon credits for a list of trips
     * @param trips List of trips to calculate
     * @return CarbonCalculationResult with detailed calculation
     */
    @Transactional(readOnly = true)
    public CarbonCalculationResult calculate(List<Trip> trips) {
        log.debug("Calculating carbon credits for {} trips", trips.size());
        
        if (trips == null || trips.isEmpty()) {
            return CarbonCalculationResult.builder()
                    .totalDistanceKm(BigDecimal.ZERO)
                    .co2SavedKg(BigDecimal.ZERO)
                    .creditAmountTons(BigDecimal.ZERO)
                    .tripCount(0)
                    .methodology("CDM ACM0018")
                    .calculationDate(LocalDateTime.now())
                    .build();
        }
        
        // Calculate total distance
        BigDecimal totalDistanceKm = trips.stream()
                .map(Trip::getDistanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate net reduction factor (ICE - EV emissions)
        BigDecimal netReductionFactor = ICE_EMISSION_FACTOR.subtract(EV_EMISSION_FACTOR);
        
        // Calculate CO2 saved in kg with precision
        BigDecimal co2SavedKg = totalDistanceKm
                .multiply(netReductionFactor)
                .setScale(4, RoundingMode.HALF_UP);
        
        // Convert to metric tons
        BigDecimal creditTons = co2SavedKg
                .divide(KG_TO_TONS, 4, RoundingMode.HALF_UP);
        
        // Calculate average data quality score
        BigDecimal avgDataQuality = calculateAverageDataQuality(trips);
        
        // Apply data quality adjustment if needed
        if (avgDataQuality.compareTo(new BigDecimal("0.95")) < 0) {
            creditTons = creditTons.multiply(avgDataQuality)
                    .setScale(4, RoundingMode.HALF_UP);
            log.info("Applied data quality adjustment factor: {}", avgDataQuality);
        }
        
        // Build detailed calculation result
        CarbonCalculationResult.CalculationDetails details = CarbonCalculationResult.CalculationDetails.builder()
                .iceEmissionFactor(ICE_EMISSION_FACTOR)
                .evEmissionFactor(EV_EMISSION_FACTOR)
                .netReductionFactor(netReductionFactor)
                .averageTripDistance(calculateAverageTripDistance(trips))
                .dataQualityScore(avgDataQuality)
                .calculationMethod("Distance-based emission reduction")
                .verificationStandard("CDM ACM0018")
                .build();
        
        CarbonCalculationResult result = CarbonCalculationResult.builder()
                .totalDistanceKm(totalDistanceKm)
                .co2SavedKg(co2SavedKg)
                .creditAmountTons(creditTons)
                .tripCount(trips.size())
                .methodology("CDM ACM0018")
                .calculationDate(LocalDateTime.now())
                .details(details)
                .build();
        
        log.info("Calculation completed: {} km, {} kg CO2 saved, {} tons credits",
                totalDistanceKm, co2SavedKg, creditTons);
        
        return result;
    }
    
    /**
     * Calculate carbon credits based on request parameters
     */
    @Transactional(readOnly = true)
    public CarbonCalculationResult calculateFromRequest(CalculateCarbonRequest request) {
        List<Trip> trips;
        
        if (request.getTripIds() != null && !request.getTripIds().isEmpty()) {
            // Calculate for specific trips
            trips = tripRepository.findByTripIdIn(request.getTripIds());
        } else {
            // Calculate for date range
            LocalDateTime startTime = request.getStartDate().atStartOfDay();
            LocalDateTime endTime = request.getEndDate().plusDays(1).atStartOfDay();
            
            trips = tripRepository.findTripsForCalculation(
                    request.getVehicleId(),
                    startTime,
                    endTime,
                    request.getIncludeAnomalousTrips()
            );
        }
        
        CarbonCalculationResult result = calculate(trips);
        
        // Validate against expected values if provided
        if (request.getTotalDistanceKm() != null) {
            BigDecimal difference = result.getTotalDistanceKm()
                    .subtract(request.getTotalDistanceKm())
                    .abs();
            BigDecimal tolerance = request.getTotalDistanceKm()
                    .multiply(ACCURACY_TOLERANCE);
            
            if (difference.compareTo(tolerance) > 0) {
                log.warn("Calculated distance {} differs from expected {} by more than {}%",
                        result.getTotalDistanceKm(), request.getTotalDistanceKm(),
                        ACCURACY_TOLERANCE.multiply(new BigDecimal("100")));
            }
        }
        
        return result;
    }
    
    /**
     * Validate calculation accuracy within ±2% tolerance
     */
    public boolean validateAccuracy(BigDecimal calculated, BigDecimal expected) {
        if (expected.compareTo(BigDecimal.ZERO) == 0) {
            return calculated.compareTo(BigDecimal.ZERO) == 0;
        }
        
        BigDecimal difference = calculated.subtract(expected).abs();
        BigDecimal tolerance = expected.multiply(ACCURACY_TOLERANCE);
        
        boolean isAccurate = difference.compareTo(tolerance) <= 0;
        
        if (!isAccurate) {
            log.error("Accuracy validation failed: calculated={}, expected={}, difference={}, tolerance={}",
                    calculated, expected, difference, tolerance);
        }
        
        return isAccurate;
    }
    
    /**
     * Calculate average data quality score for trips
     */
    private BigDecimal calculateAverageDataQuality(List<Trip> trips) {
        if (trips.isEmpty()) {
            return BigDecimal.ONE;
        }
        
        BigDecimal totalQuality = trips.stream()
                .map(trip -> trip.getDataQualityScore() != null ? 
                        trip.getDataQualityScore() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalQuality.divide(
                new BigDecimal(trips.size()), 
                4, 
                RoundingMode.HALF_UP
        );
    }
    
    /**
     * Calculate average trip distance
     */
    private BigDecimal calculateAverageTripDistance(List<Trip> trips) {
        if (trips.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalDistance = trips.stream()
                .map(Trip::getDistanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalDistance.divide(
                new BigDecimal(trips.size()), 
                2, 
                RoundingMode.HALF_UP
        );
    }
    
    /**
     * Quick calculation for estimation (without trips)
     */
    public CarbonCalculationResult quickCalculate(BigDecimal distanceKm) {
        BigDecimal netReductionFactor = ICE_EMISSION_FACTOR.subtract(EV_EMISSION_FACTOR);
        BigDecimal co2SavedKg = distanceKm.multiply(netReductionFactor)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal creditTons = co2SavedKg.divide(KG_TO_TONS, 4, RoundingMode.HALF_UP);
        
        return CarbonCalculationResult.builder()
                .totalDistanceKm(distanceKm)
                .co2SavedKg(co2SavedKg)
                .creditAmountTons(creditTons)
                .methodology("CDM ACM0018")
                .calculationDate(LocalDateTime.now())
                .build();
    }
}
