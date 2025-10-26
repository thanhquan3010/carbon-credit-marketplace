package com.carbonmarketplace.verificationservice.service;

import com.carbonmarketplace.verificationservice.entity.Anomaly;
import com.carbonmarketplace.verificationservice.entity.Trip;
import com.carbonmarketplace.verificationservice.repository.AnomalyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting anomalies in trip data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final AnomalyRepository anomalyRepository;

    // Thresholds for anomaly detection
    private static final double MAX_SPEED_KMH = 150.0;
    private static final double MIN_SPEED_KMH = 5.0;
    private static final double MAX_EFFICIENCY_KWH_PER_100KM = 30.0;
    private static final double MIN_EFFICIENCY_KWH_PER_100KM = 10.0;
    private static final double GPS_JUMP_THRESHOLD_KM = 100.0;
    private static final double OUTLIER_THRESHOLD_STDDEV = 3.0;
    private static final double TIME_OVERLAP_TOLERANCE_MINUTES = 5.0;

    /**
     * Detect anomalies in a list of trips.
     */
    @Transactional
    public List<Anomaly> detectAnomalies(UUID verificationId, List<Trip> trips) {
        log.info("Starting anomaly detection for verification {} with {} trips", verificationId, trips.size());
        
        List<Anomaly> anomalies = new ArrayList<>();
        
        // Perform different types of anomaly checks
        anomalies.addAll(checkSpeedAnomalies(verificationId, trips));
        anomalies.addAll(checkDuplicateTrips(verificationId, trips));
        anomalies.addAll(checkEnergyEfficiencyOutliers(verificationId, trips));
        anomalies.addAll(checkTimeOverlaps(verificationId, trips));
        anomalies.addAll(checkGPSJumps(verificationId, trips));
        anomalies.addAll(checkDistanceTimeConsistency(verificationId, trips));
        anomalies.addAll(checkDataCompleteness(verificationId, trips));
        
        // Save all detected anomalies
        if (!anomalies.isEmpty()) {
            anomalies = anomalyRepository.saveAll(anomalies);
            log.warn("Detected {} anomalies for verification {}", anomalies.size(), verificationId);
        } else {
            log.info("No anomalies detected for verification {}", verificationId);
        }
        
        return anomalies;
    }

    /**
     * Check for speed anomalies (too high or too low).
     */
    private List<Anomaly> checkSpeedAnomalies(UUID verificationId, List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        for (Trip trip : trips) {
            if (trip.getAvgSpeed() == null) continue;
            
            double speed = trip.getAvgSpeed().doubleValue();
            
            if (speed > MAX_SPEED_KMH) {
                anomalies.add(Anomaly.builder()
                        .verificationId(verificationId)
                        .tripId(trip.getTripId())
                        .anomalyType(Anomaly.AnomalyType.HIGH_SPEED)
                        .severity(Anomaly.Severity.CRITICAL)
                        .description(String.format("Average speed %.2f km/h exceeds maximum threshold of %.2f km/h", 
                                speed, MAX_SPEED_KMH))
                        .detectedValue(String.valueOf(speed))
                        .expectedValue("<= " + MAX_SPEED_KMH)
                        .deviationPercentage((speed - MAX_SPEED_KMH) / MAX_SPEED_KMH * 100)
                        .build());
            } else if (speed < MIN_SPEED_KMH && trip.getDistanceKm().doubleValue() > 1.0) {
                anomalies.add(Anomaly.builder()
                        .verificationId(verificationId)
                        .tripId(trip.getTripId())
                        .anomalyType(Anomaly.AnomalyType.LOW_SPEED)
                        .severity(Anomaly.Severity.MEDIUM)
                        .description(String.format("Average speed %.2f km/h below minimum threshold of %.2f km/h", 
                                speed, MIN_SPEED_KMH))
                        .detectedValue(String.valueOf(speed))
                        .expectedValue(">= " + MIN_SPEED_KMH)
                        .deviationPercentage((MIN_SPEED_KMH - speed) / MIN_SPEED_KMH * 100)
                        .build());
            }
        }
        
        return anomalies;
    }

    /**
     * Check for duplicate trips based on time and distance.
     */
    private List<Anomaly> checkDuplicateTrips(UUID verificationId, List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        Map<String, List<Trip>> tripGroups = new HashMap<>();
        
        // Group trips by similar characteristics
        for (Trip trip : trips) {
            String key = String.format("%s-%s-%.1f",
                    trip.getStartTime().toLocalDate(),
                    trip.getEndTime().toLocalDate(),
                    trip.getDistanceKm().setScale(1, RoundingMode.HALF_UP).doubleValue());
            
            tripGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(trip);
        }
        
        // Find duplicates
        for (Map.Entry<String, List<Trip>> entry : tripGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (Trip trip : entry.getValue()) {
                    anomalies.add(Anomaly.builder()
                            .verificationId(verificationId)
                            .tripId(trip.getTripId())
                            .anomalyType(Anomaly.AnomalyType.DUPLICATE_TRIP)
                            .severity(Anomaly.Severity.HIGH)
                            .description(String.format("Potential duplicate trip detected. %d similar trips found on %s",
                                    entry.getValue().size(), trip.getStartTime().toLocalDate()))
                            .detectedValue(entry.getKey())
                            .expectedValue("Unique trips")
                            .build());
                }
            }
        }
        
        return anomalies;
    }

    /**
     * Check for energy efficiency outliers using statistical analysis.
     */
    private List<Anomaly> checkEnergyEfficiencyOutliers(UUID verificationId, List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        // Calculate efficiency for trips with energy data
        List<Double> efficiencies = trips.stream()
                .filter(t -> t.getEnergyConsumedKwh() != null && 
                           t.getEnergyConsumedKwh().compareTo(BigDecimal.ZERO) > 0)
                .map(t -> calculateEfficiency(t))
                .filter(e -> e > 0)
                .collect(Collectors.toList());
        
        if (efficiencies.size() < 3) {
            return anomalies; // Not enough data for statistical analysis
        }
        
        // Calculate statistics
        DescriptiveStatistics stats = new DescriptiveStatistics();
        efficiencies.forEach(stats::addValue);
        
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double lowerBound = mean - (OUTLIER_THRESHOLD_STDDEV * stdDev);
        double upperBound = mean + (OUTLIER_THRESHOLD_STDDEV * stdDev);
        
        // Check each trip for outliers
        for (Trip trip : trips) {
            if (trip.getEnergyConsumedKwh() == null || 
                trip.getEnergyConsumedKwh().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            
            double efficiency = calculateEfficiency(trip);
            
            if (efficiency < lowerBound || efficiency > upperBound) {
                Anomaly.Severity severity = Math.abs(efficiency - mean) > (4 * stdDev) 
                        ? Anomaly.Severity.HIGH 
                        : Anomaly.Severity.MEDIUM;
                
                anomalies.add(Anomaly.builder()
                        .verificationId(verificationId)
                        .tripId(trip.getTripId())
                        .anomalyType(Anomaly.AnomalyType.EFFICIENCY_OUTLIER)
                        .severity(severity)
                        .description(String.format("Energy efficiency %.2f kWh/100km is an outlier (mean: %.2f, stddev: %.2f)",
                                efficiency, mean, stdDev))
                        .detectedValue(String.format("%.2f", efficiency))
                        .expectedValue(String.format("%.2f - %.2f", lowerBound, upperBound))
                        .deviationPercentage(Math.abs(efficiency - mean) / mean * 100)
                        .build());
            }
            
            // Also check against absolute thresholds
            if (efficiency > MAX_EFFICIENCY_KWH_PER_100KM || efficiency < MIN_EFFICIENCY_KWH_PER_100KM) {
                anomalies.add(Anomaly.builder()
                        .verificationId(verificationId)
                        .tripId(trip.getTripId())
                        .anomalyType(Anomaly.AnomalyType.ENERGY_CONSUMPTION_OUTLIER)
                        .severity(Anomaly.Severity.CRITICAL)
                        .description(String.format("Energy efficiency %.2f kWh/100km outside acceptable range (%.2f - %.2f)",
                                efficiency, MIN_EFFICIENCY_KWH_PER_100KM, MAX_EFFICIENCY_KWH_PER_100KM))
                        .detectedValue(String.format("%.2f", efficiency))
                        .expectedValue(String.format("%.2f - %.2f", MIN_EFFICIENCY_KWH_PER_100KM, MAX_EFFICIENCY_KWH_PER_100KM))
                        .build());
            }
        }
        
        return anomalies;
    }

    /**
     * Check for overlapping trips (vehicle in two places at once).
     */
    private List<Anomaly> checkTimeOverlaps(UUID verificationId, List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        // Sort trips by start time
        List<Trip> sortedTrips = new ArrayList<>(trips);
        sortedTrips.sort(Comparator.comparing(Trip::getStartTime));
        
        for (int i = 0; i < sortedTrips.size() - 1; i++) {
            Trip current = sortedTrips.get(i);
            Trip next = sortedTrips.get(i + 1);
            
            // Check for overlap
            if (current.getEndTime().isAfter(next.getStartTime())) {
                long overlapMinutes = Duration.between(next.getStartTime(), current.getEndTime()).toMinutes();
                
                if (overlapMinutes > TIME_OVERLAP_TOLERANCE_MINUTES) {
                    Anomaly.Severity severity = overlapMinutes > 30 
                            ? Anomaly.Severity.CRITICAL 
                            : Anomaly.Severity.HIGH;
                    
                    anomalies.add(Anomaly.builder()
                            .verificationId(verificationId)
                            .tripId(current.getTripId())
                            .anomalyType(Anomaly.AnomalyType.TIME_OVERLAP)
                            .severity(severity)
                            .description(String.format("Trip overlaps with next trip by %d minutes", overlapMinutes))
                            .detectedValue(String.format("%d minutes overlap", overlapMinutes))
                            .expectedValue("No overlap")
                            .build());
                    
                    anomalies.add(Anomaly.builder()
                            .verificationId(verificationId)
                            .tripId(next.getTripId())
                            .anomalyType(Anomaly.AnomalyType.TIME_OVERLAP)
                            .severity(severity)
                            .description(String.format("Trip overlaps with previous trip by %d minutes", overlapMinutes))
                            .detectedValue(String.format("%d minutes overlap", overlapMinutes))
                            .expectedValue("No overlap")
                            .build());
                }
            }
        }
        
        return anomalies;
    }

    /**
     * Check for impossible GPS jumps between trips.
     */
    private List<Anomaly> checkGPSJumps(UUID verificationId, List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        // Sort trips by start time
        List<Trip> sortedTrips = trips.stream()
                .filter(t -> t.getEndLat() != null && t.getEndLng() != null && 
                           t.getStartLat() != null && t.getStartLng() != null)
                .sorted(Comparator.comparing(Trip::getStartTime))
                .collect(Collectors.toList());
        
        for (int i = 0; i < sortedTrips.size() - 1; i++) {
            Trip current = sortedTrips.get(i);
            Trip next = sortedTrips.get(i + 1);
            
            // Calculate distance between end of current trip and start of next trip
            double distance = calculateHaversineDistance(
                    current.getEndLat().doubleValue(),
                    current.getEndLng().doubleValue(),
                    next.getStartLat().doubleValue(),
                    next.getStartLng().doubleValue()
            );
            
            // Calculate time between trips
            long minutesBetween = Duration.between(current.getEndTime(), next.getStartTime()).toMinutes();
            
            // Check if jump is impossible (e.g., 100km in 5 minutes)
            if (minutesBetween > 0 && minutesBetween < 60) {
                double maxPossibleDistance = (minutesBetween / 60.0) * MAX_SPEED_KMH;
                
                if (distance > maxPossibleDistance && distance > GPS_JUMP_THRESHOLD_KM) {
                    anomalies.add(Anomaly.builder()
                            .verificationId(verificationId)
                            .tripId(next.getTripId())
                            .anomalyType(Anomaly.AnomalyType.GPS_JUMP)
                            .severity(Anomaly.Severity.HIGH)
                            .description(String.format("GPS jump of %.2f km in %d minutes between consecutive trips",
                                    distance, minutesBetween))
                            .detectedValue(String.format("%.2f km", distance))
                            .expectedValue(String.format("<= %.2f km", maxPossibleDistance))
                            .deviationPercentage((distance - maxPossibleDistance) / maxPossibleDistance * 100)
                            .build());
                }
            }
        }
        
        return anomalies;
    }

    /**
     * Check distance vs time consistency.
     */
    private List<Anomaly> checkDistanceTimeConsistency(UUID verificationId, List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        for (Trip trip : trips) {
            if (trip.getDistanceKm() == null || trip.getDurationSeconds() == null || 
                trip.getDurationSeconds() == 0) {
                continue;
            }
            
            double distance = trip.getDistanceKm().doubleValue();
            double hours = trip.getDurationSeconds() / 3600.0;
            double calculatedSpeed = distance / hours;
            
            // Check if calculated speed matches reported average speed
            if (trip.getAvgSpeed() != null) {
                double reportedSpeed = trip.getAvgSpeed().doubleValue();
                double speedDifference = Math.abs(calculatedSpeed - reportedSpeed);
                double speedDifferencePercent = (speedDifference / reportedSpeed) * 100;
                
                if (speedDifferencePercent > 20) {
                    anomalies.add(Anomaly.builder()
                            .verificationId(verificationId)
                            .tripId(trip.getTripId())
                            .anomalyType(Anomaly.AnomalyType.DISTANCE_MISMATCH)
                            .severity(speedDifferencePercent > 50 ? Anomaly.Severity.HIGH : Anomaly.Severity.MEDIUM)
                            .description(String.format("Distance/time inconsistency: calculated speed %.2f km/h differs from reported %.2f km/h by %.1f%%",
                                    calculatedSpeed, reportedSpeed, speedDifferencePercent))
                            .detectedValue(String.format("%.2f km/h", calculatedSpeed))
                            .expectedValue(String.format("%.2f km/h", reportedSpeed))
                            .deviationPercentage(speedDifferencePercent)
                            .build());
                }
            }
        }
        
        return anomalies;
    }

    /**
     * Check for missing or incomplete data.
     */
    private List<Anomaly> checkDataCompleteness(UUID verificationId, List<Trip> trips) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        for (Trip trip : trips) {
            List<String> missingFields = new ArrayList<>();
            
            if (trip.getStartTime() == null) missingFields.add("start_time");
            if (trip.getEndTime() == null) missingFields.add("end_time");
            if (trip.getDistanceKm() == null) missingFields.add("distance");
            if (trip.getCo2SavedKg() == null) missingFields.add("co2_saved");
            
            if (!missingFields.isEmpty()) {
                anomalies.add(Anomaly.builder()
                        .verificationId(verificationId)
                        .tripId(trip.getTripId())
                        .anomalyType(Anomaly.AnomalyType.MISSING_DATA)
                        .severity(missingFields.size() > 2 ? Anomaly.Severity.HIGH : Anomaly.Severity.MEDIUM)
                        .description("Missing required data fields: " + String.join(", ", missingFields))
                        .detectedValue("Missing: " + missingFields.size() + " fields")
                        .expectedValue("All fields present")
                        .build());
            }
        }
        
        return anomalies;
    }

    /**
     * Calculate energy efficiency in kWh per 100km.
     */
    private double calculateEfficiency(Trip trip) {
        if (trip.getEnergyConsumedKwh() == null || trip.getDistanceKm() == null ||
            trip.getDistanceKm().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        
        return trip.getEnergyConsumedKwh().doubleValue() / trip.getDistanceKm().doubleValue() * 100;
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula.
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Radius of the Earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
