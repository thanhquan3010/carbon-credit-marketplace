package com.carbonmarketplace.verificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a trip (read-only from vehicle service).
 * Used for verification and anomaly detection.
 */
@Entity
@Table(name = "trips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    // Trip Details
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Location
    @Column(name = "start_lat", precision = 10, scale = 7)
    private BigDecimal startLat;

    @Column(name = "start_lng", precision = 10, scale = 7)
    private BigDecimal startLng;

    @Column(name = "end_lat", precision = 10, scale = 7)
    private BigDecimal endLat;

    @Column(name = "end_lng", precision = 10, scale = 7)
    private BigDecimal endLng;

    @Column(name = "start_address")
    private String startAddress;

    @Column(name = "end_address")
    private String endAddress;

    // Distance & Energy
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "avg_speed", precision = 10, scale = 2)
    private BigDecimal avgSpeed;

    @Column(name = "energy_consumed_kwh", precision = 10, scale = 2)
    private BigDecimal energyConsumedKwh;

    // Carbon Calculation
    @Column(name = "co2_saved_kg", nullable = false, precision = 10, scale = 4)
    private BigDecimal co2SavedKg;

    // Data Quality
    @Column(name = "data_source")
    private String dataSource;

    @Column(name = "data_quality_score", precision = 3, scale = 2)
    private BigDecimal dataQualityScore;

    @Column(name = "has_anomaly")
    private Boolean hasAnomaly = false;

    @Column(name = "anomaly_description")
    private String anomalyDescription;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;

    @Column(name = "verification_id")
    private UUID verificationId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Calculate energy efficiency (km/kWh)
     */
    public BigDecimal getEnergyEfficiency() {
        if (energyConsumedKwh == null || energyConsumedKwh.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return distanceKm.divide(energyConsumedKwh, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calculate trip duration in hours
     */
    public BigDecimal getDurationHours() {
        if (durationSeconds == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(durationSeconds).divide(new BigDecimal(3600), 2, BigDecimal.ROUND_HALF_UP);
    }
}
