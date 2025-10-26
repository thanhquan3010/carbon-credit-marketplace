package com.carbonmarketplace.vehicleservice.dto.response;

import com.carbonmarketplace.vehicleservice.entity.Trip;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for trip information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private UUID tripId;
    private UUID vehicleId;
    private String externalTripId;

    // Trip Details
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    private Integer durationSeconds;

    // Location
    private BigDecimal startLat;
    private BigDecimal startLng;
    private BigDecimal endLat;
    private BigDecimal endLng;
    private String startAddress;
    private String endAddress;

    // Distance & Energy
    private BigDecimal distanceKm;
    private BigDecimal avgSpeed;
    private BigDecimal maxSpeed;
    private BigDecimal energyConsumedKwh;
    private BigDecimal efficiencyKwhPerKm;

    // Carbon Calculation
    private BigDecimal co2SavedKg;

    // Data Quality
    private Trip.DataSource dataSource;
    private BigDecimal dataQualityScore;
    private Boolean hasAnomaly;
    private String anomalyDescription;

    // Verification
    private Boolean isVerified;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime verifiedAt;
    
    private String verificationNotes;

    // Additional metrics
    private Integer idleTimeSeconds;
    private Integer drivingTimeSeconds;
    private Integer chargingTimeSeconds;
    private BigDecimal regenerationKwh;
    private Integer ambientTemperature;
    private Integer batteryLevelStart;
    private Integer batteryLevelEnd;

    // Audit
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Convert Trip entity to TripResponse DTO
     */
    public static TripResponse fromEntity(Trip trip) {
        return TripResponse.builder()
                .tripId(trip.getTripId())
                .vehicleId(trip.getVehicleId())
                .externalTripId(trip.getExternalTripId())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .durationSeconds(trip.getDurationSeconds())
                .startLat(trip.getStartLat())
                .startLng(trip.getStartLng())
                .endLat(trip.getEndLat())
                .endLng(trip.getEndLng())
                .startAddress(trip.getStartAddress())
                .endAddress(trip.getEndAddress())
                .distanceKm(trip.getDistanceKm())
                .avgSpeed(trip.getAvgSpeed())
                .maxSpeed(trip.getMaxSpeed())
                .energyConsumedKwh(trip.getEnergyConsumedKwh())
                .efficiencyKwhPerKm(trip.getEfficiencyKwhPerKm())
                .co2SavedKg(trip.getCo2SavedKg())
                .dataSource(trip.getDataSource())
                .dataQualityScore(trip.getDataQualityScore())
                .hasAnomaly(trip.getHasAnomaly())
                .anomalyDescription(trip.getAnomalyDescription())
                .isVerified(trip.getIsVerified())
                .verifiedAt(trip.getVerifiedAt())
                .verificationNotes(trip.getVerificationNotes())
                .idleTimeSeconds(trip.getIdleTimeSeconds())
                .drivingTimeSeconds(trip.getDrivingTimeSeconds())
                .chargingTimeSeconds(trip.getChargingTimeSeconds())
                .regenerationKwh(trip.getRegenerationKwh())
                .ambientTemperature(trip.getAmbientTemperature())
                .batteryLevelStart(trip.getBatteryLevelStart())
                .batteryLevelEnd(trip.getBatteryLevelEnd())
                .createdAt(trip.getCreatedAt())
                .build();
    }
}
