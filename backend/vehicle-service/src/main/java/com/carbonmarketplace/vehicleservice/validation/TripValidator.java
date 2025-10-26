package com.carbonmarketplace.vehicleservice.validation;

import com.carbonmarketplace.vehicleservice.entity.Trip;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for trip data quality and anomaly detection
 * Implements validation rules as specified in requirements
 */
@Component
@Slf4j
public class TripValidator {

    // Validation thresholds
    private static final BigDecimal MIN_DISTANCE_KM = new BigDecimal("0.1");
    private static final BigDecimal MAX_DISTANCE_KM = new BigDecimal("500");
    private static final BigDecimal MAX_AVG_SPEED_KMH = new BigDecimal("180");
    private static final BigDecimal MAX_MAX_SPEED_KMH = new BigDecimal("200");
    private static final BigDecimal MIN_EFFICIENCY_KWH_PER_100KM = new BigDecimal("10");
    private static final BigDecimal MAX_EFFICIENCY_KWH_PER_100KM = new BigDecimal("25");
    private static final long MIN_DURATION_MINUTES = 2;
    private static final long MAX_DURATION_HOURS = 24;

    /**
     * Validate a trip and return validation result
     */
    public ValidationResult validate(Trip trip) {
        log.debug("Validating trip: {}", trip.getTripId());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate required fields
        if (trip.getStartTime() == null || trip.getEndTime() == null) {
            errors.add("Start time and end time are required");
            return ValidationResult.invalid(String.join("; ", errors));
        }
        
        if (trip.getDistanceKm() == null) {
            errors.add("Distance is required");
            return ValidationResult.invalid(String.join("; ", errors));
        }
        
        // Validate time consistency
        if (trip.getEndTime().isBefore(trip.getStartTime()) || trip.getEndTime().isEqual(trip.getStartTime())) {
            errors.add("End time must be after start time");
        }
        
        // Validate duration
        Duration duration = Duration.between(trip.getStartTime(), trip.getEndTime());
        long durationMinutes = duration.toMinutes();
        
        if (durationMinutes < MIN_DURATION_MINUTES) {
            errors.add(String.format("Trip too short (<%d minutes)", MIN_DURATION_MINUTES));
        }
        
        if (duration.toHours() > MAX_DURATION_HOURS) {
            warnings.add(String.format("Unusually long trip (>%d hours)", MAX_DURATION_HOURS));
        }
        
        // Validate distance
        if (trip.getDistanceKm().compareTo(MIN_DISTANCE_KM) < 0) {
            errors.add(String.format("Distance too short (<%.1f km)", MIN_DISTANCE_KM));
        }
        
        if (trip.getDistanceKm().compareTo(MAX_DISTANCE_KM) > 0) {
            errors.add(String.format("Distance too long (>%.0f km)", MAX_DISTANCE_KM));
        }
        
        // Validate average speed
        if (trip.getAvgSpeed() != null) {
            if (trip.getAvgSpeed().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Average speed cannot be negative");
            }
            
            if (trip.getAvgSpeed().compareTo(MAX_AVG_SPEED_KMH) > 0) {
                errors.add(String.format("Average speed too high (>%.0f km/h)", MAX_AVG_SPEED_KMH));
            }
            
            // Check speed consistency with distance and time
            if (durationMinutes > 0) {
                BigDecimal calculatedSpeed = trip.getDistanceKm()
                        .multiply(BigDecimal.valueOf(60))
                        .divide(BigDecimal.valueOf(durationMinutes), 2, BigDecimal.ROUND_HALF_UP);
                
                if (trip.getAvgSpeed().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal speedDifference = calculatedSpeed.subtract(trip.getAvgSpeed()).abs();
                    BigDecimal speedTolerance = calculatedSpeed.multiply(new BigDecimal("0.1")); // 10% tolerance
                    
                    if (speedDifference.compareTo(speedTolerance) > 0) {
                        warnings.add("Average speed inconsistent with distance and duration");
                    }
                }
            }
        }
        
        // Validate max speed
        if (trip.getMaxSpeed() != null) {
            if (trip.getMaxSpeed().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Maximum speed cannot be negative");
            }
            
            if (trip.getMaxSpeed().compareTo(MAX_MAX_SPEED_KMH) > 0) {
                warnings.add(String.format("Maximum speed unusually high (>%.0f km/h)", MAX_MAX_SPEED_KMH));
            }
            
            // Max speed should be >= avg speed
            if (trip.getAvgSpeed() != null && trip.getMaxSpeed().compareTo(trip.getAvgSpeed()) < 0) {
                errors.add("Maximum speed cannot be less than average speed");
            }
        }
        
        // Validate energy efficiency
        if (trip.getEnergyConsumedKwh() != null && trip.getDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal efficiencyPer100Km = trip.getEnergyConsumedKwh()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(trip.getDistanceKm(), 2, BigDecimal.ROUND_HALF_UP);
            
            if (efficiencyPer100Km.compareTo(MIN_EFFICIENCY_KWH_PER_100KM) < 0) {
                warnings.add(String.format("Unusually low energy consumption (<%.0f kWh/100km)", MIN_EFFICIENCY_KWH_PER_100KM));
            }
            
            if (efficiencyPer100Km.compareTo(MAX_EFFICIENCY_KWH_PER_100KM) > 0) {
                warnings.add(String.format("Unusually high energy consumption (>%.0f kWh/100km)", MAX_EFFICIENCY_KWH_PER_100KM));
            }
        }
        
        // Validate coordinates if provided
        if (trip.getStartLat() != null && trip.getStartLng() != null) {
            if (trip.getStartLat().compareTo(new BigDecimal("-90")) < 0 || 
                trip.getStartLat().compareTo(new BigDecimal("90")) > 0) {
                errors.add("Invalid start latitude");
            }
            
            if (trip.getStartLng().compareTo(new BigDecimal("-180")) < 0 || 
                trip.getStartLng().compareTo(new BigDecimal("180")) > 0) {
                errors.add("Invalid start longitude");
            }
        }
        
        if (trip.getEndLat() != null && trip.getEndLng() != null) {
            if (trip.getEndLat().compareTo(new BigDecimal("-90")) < 0 || 
                trip.getEndLat().compareTo(new BigDecimal("90")) > 0) {
                errors.add("Invalid end latitude");
            }
            
            if (trip.getEndLng().compareTo(new BigDecimal("-180")) < 0 || 
                trip.getEndLng().compareTo(new BigDecimal("180")) > 0) {
                errors.add("Invalid end longitude");
            }
        }
        
        // Validate battery levels if provided
        if (trip.getBatteryLevelStart() != null) {
            if (trip.getBatteryLevelStart() < 0 || trip.getBatteryLevelStart() > 100) {
                warnings.add("Invalid starting battery level");
            }
        }
        
        if (trip.getBatteryLevelEnd() != null) {
            if (trip.getBatteryLevelEnd() < 0 || trip.getBatteryLevelEnd() > 100) {
                warnings.add("Invalid ending battery level");
            }
        }
        
        if (trip.getBatteryLevelStart() != null && trip.getBatteryLevelEnd() != null) {
            // Battery level should decrease or stay same (unless charged during trip)
            if (trip.getBatteryLevelEnd() > trip.getBatteryLevelStart() && 
                (trip.getChargingTimeSeconds() == null || trip.getChargingTimeSeconds() == 0)) {
                warnings.add("Battery level increased without charging time recorded");
            }
        }
        
        // Check for future dates
        if (trip.getStartTime().isAfter(LocalDateTime.now())) {
            errors.add("Trip start time cannot be in the future");
        }
        
        if (trip.getEndTime().isAfter(LocalDateTime.now())) {
            errors.add("Trip end time cannot be in the future");
        }
        
        // Check for very old dates (potential data error)
        if (trip.getStartTime().isBefore(LocalDateTime.now().minusYears(5))) {
            warnings.add("Trip date is more than 5 years old");
        }
        
        // Return validation result
        if (!errors.isEmpty()) {
            return ValidationResult.invalid(String.join("; ", errors));
        } else if (!warnings.isEmpty()) {
            return ValidationResult.warning(String.join("; ", warnings));
        } else {
            return ValidationResult.valid();
        }
    }
    
    /**
     * Batch validate trips
     */
    public List<ValidationResult> validateBatch(List<Trip> trips) {
        log.info("Validating batch of {} trips", trips.size());
        
        List<ValidationResult> results = new ArrayList<>();
        for (Trip trip : trips) {
            results.add(validate(trip));
        }
        
        return results;
    }
    
    /**
     * Detect anomalies in a set of trips
     */
    public List<String> detectAnomalies(List<Trip> trips) {
        List<String> anomalies = new ArrayList<>();
        
        if (trips.isEmpty()) {
            return anomalies;
        }
        
        // Check for duplicate trips (same start and end time)
        for (int i = 0; i < trips.size() - 1; i++) {
            for (int j = i + 1; j < trips.size(); j++) {
                Trip trip1 = trips.get(i);
                Trip trip2 = trips.get(j);
                
                if (trip1.getStartTime().equals(trip2.getStartTime()) && 
                    trip1.getEndTime().equals(trip2.getEndTime())) {
                    anomalies.add(String.format("Duplicate trips found: %s and %s", 
                            trip1.getTripId(), trip2.getTripId()));
                }
            }
        }
        
        // Check for overlapping trips
        for (int i = 0; i < trips.size() - 1; i++) {
            for (int j = i + 1; j < trips.size(); j++) {
                Trip trip1 = trips.get(i);
                Trip trip2 = trips.get(j);
                
                if (isOverlapping(trip1, trip2)) {
                    anomalies.add(String.format("Overlapping trips found: %s and %s", 
                            trip1.getTripId(), trip2.getTripId()));
                }
            }
        }
        
        // Check for impossible travel (teleportation)
        trips.sort((t1, t2) -> t1.getStartTime().compareTo(t2.getStartTime()));
        for (int i = 0; i < trips.size() - 1; i++) {
            Trip trip1 = trips.get(i);
            Trip trip2 = trips.get(i + 1);
            
            if (trip1.getEndLat() != null && trip1.getEndLng() != null &&
                trip2.getStartLat() != null && trip2.getStartLng() != null) {
                
                BigDecimal distance = calculateDistance(
                        trip1.getEndLat(), trip1.getEndLng(),
                        trip2.getStartLat(), trip2.getStartLng()
                );
                
                Duration timeBetween = Duration.between(trip1.getEndTime(), trip2.getStartTime());
                
                if (timeBetween.toMinutes() > 0) {
                    BigDecimal maxPossibleSpeed = distance
                            .multiply(BigDecimal.valueOf(60))
                            .divide(BigDecimal.valueOf(timeBetween.toMinutes()), 2, BigDecimal.ROUND_HALF_UP);
                    
                    if (maxPossibleSpeed.compareTo(new BigDecimal("200")) > 0) {
                        anomalies.add(String.format("Impossible travel speed between trips %s and %s", 
                                trip1.getTripId(), trip2.getTripId()));
                    }
                }
            }
        }
        
        return anomalies;
    }
    
    /**
     * Check if two trips overlap in time
     */
    private boolean isOverlapping(Trip trip1, Trip trip2) {
        return (trip1.getStartTime().isBefore(trip2.getEndTime()) && 
                trip1.getEndTime().isAfter(trip2.getStartTime()));
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1, 
                                          BigDecimal lat2, BigDecimal lon2) {
        final int R = 6371; // Earth's radius in kilometers
        
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double deltaLon = Math.toRadians(lon2.subtract(lon1).doubleValue());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        
        return BigDecimal.valueOf(distance);
    }
    
    /**
     * Validation result class
     */
    @Data
    @Builder
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> errors;
        private final List<String> warnings;
        
        public static ValidationResult valid() {
            return ValidationResult.builder()
                    .valid(true)
                    .errors(new ArrayList<>())
                    .warnings(new ArrayList<>())
                    .build();
        }
        
        public static ValidationResult invalid(String message) {
            return ValidationResult.builder()
                    .valid(false)
                    .message(message)
                    .errors(List.of(message))
                    .warnings(new ArrayList<>())
                    .build();
        }
        
        public static ValidationResult warning(String message) {
            return ValidationResult.builder()
                    .valid(true)
                    .message(message)
                    .errors(new ArrayList<>())
                    .warnings(List.of(message))
                    .build();
        }
        
        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }
        
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }
}

