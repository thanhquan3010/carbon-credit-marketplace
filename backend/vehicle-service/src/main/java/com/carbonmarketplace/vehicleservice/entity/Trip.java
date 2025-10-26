package com.carbonmarketplace.vehicleservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing individual EV trips for carbon credit calculation
 * Note: This table should be partitioned by month for scalability
 */
@Entity
@Table(name = "trips", indexes = {
        @Index(name = "idx_trip_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_trip_start_time", columnList = "start_time"),
        @Index(name = "idx_trip_end_time", columnList = "end_time"),
        @Index(name = "idx_trip_sync_batch", columnList = "sync_batch_id"),
        @Index(name = "idx_trip_anomaly", columnList = "has_anomaly"),
        @Index(name = "idx_trip_vehicle_date", columnList = "vehicle_id, start_time")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"vehicle", "rawData"})
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trip_id", updatable = false, nullable = false)
    private UUID tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "vehicle_id", insertable = false, updatable = false)
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

    @Column(name = "start_address", columnDefinition = "TEXT")
    private String startAddress;

    @Column(name = "end_address", columnDefinition = "TEXT")
    private String endAddress;

    // Distance & Energy
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "avg_speed", precision = 10, scale = 2)
    private BigDecimal avgSpeed;

    @Column(name = "max_speed", precision = 10, scale = 2)
    private BigDecimal maxSpeed;

    @Column(name = "energy_consumed_kwh", precision = 10, scale = 2)
    private BigDecimal energyConsumedKwh;

    // Carbon Calculation
    @Column(name = "co2_saved_kg", nullable = false, precision = 10, scale = 4)
    private BigDecimal co2SavedKg;

    // Data Quality
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 50)
    private DataSource dataSource;

    @Column(name = "data_quality_score", precision = 3, scale = 2)
    private BigDecimal dataQualityScore = BigDecimal.ONE;

    @Column(name = "has_anomaly")
    private Boolean hasAnomaly = false;

    @Column(name = "anomaly_description", columnDefinition = "TEXT")
    private String anomalyDescription;

    // Sync Information
    @Column(name = "sync_batch_id")
    private UUID syncBatchId;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData; // Store original API response as JSON

    @Column(name = "external_trip_id")
    private String externalTripId; // ID from external system (VinFast API)

    // Verification
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    // Additional metrics
    @Column(name = "idle_time_seconds")
    private Integer idleTimeSeconds;

    @Column(name = "driving_time_seconds")
    private Integer drivingTimeSeconds;

    @Column(name = "charging_time_seconds")
    private Integer chargingTimeSeconds;

    @Column(name = "regeneration_kwh", precision = 10, scale = 2)
    private BigDecimal regenerationKwh;

    @Column(name = "efficiency_kwh_per_km", precision = 10, scale = 4)
    private BigDecimal efficiencyKwhPerKm;

    @Column(name = "ambient_temperature")
    private Integer ambientTemperature;

    @Column(name = "battery_level_start")
    private Integer batteryLevelStart;

    @Column(name = "battery_level_end")
    private Integer batteryLevelEnd;

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Enums
    public enum DataSource {
        API("api"),
        OBD("obd"),
        MANUAL("manual"),
        CSV_UPLOAD("csv_upload"),
        EXCEL_UPLOAD("excel_upload");

        private final String value;

        DataSource(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Helper methods
    @PrePersist
    @PreUpdate
    public void calculateDerivedFields() {
        // Calculate duration
        if (startTime != null && endTime != null) {
            durationSeconds = (int) java.time.Duration.between(startTime, endTime).getSeconds();
        }

        // Calculate average speed
        if (distanceKm != null && durationSeconds != null && durationSeconds > 0) {
            BigDecimal hours = BigDecimal.valueOf(durationSeconds).divide(BigDecimal.valueOf(3600), 2, BigDecimal.ROUND_HALF_UP);
            if (hours.compareTo(BigDecimal.ZERO) > 0) {
                avgSpeed = distanceKm.divide(hours, 2, BigDecimal.ROUND_HALF_UP);
            }
        }

        // Calculate efficiency
        if (energyConsumedKwh != null && distanceKm != null && distanceKm.compareTo(BigDecimal.ZERO) > 0) {
            efficiencyKwhPerKm = energyConsumedKwh.divide(distanceKm, 4, BigDecimal.ROUND_HALF_UP);
        }

        // Calculate CO2 saved using the standard formula
        if (distanceKm != null) {
            // CO2 Saved (kg) = Distance (km) × (ICE Emission Factor - EV Emission Factor)
            // ICE Emission Factor: 0.15 kg CO2/km
            // EV Emission Factor: 0.05 kg CO2/km
            // Net reduction: 0.10 kg CO2/km
            BigDecimal emissionReductionFactor = new BigDecimal("0.10");
            co2SavedKg = distanceKm.multiply(emissionReductionFactor).setScale(4, BigDecimal.ROUND_HALF_UP);
        }
    }

    public boolean isValid() {
        return distanceKm != null && 
               distanceKm.compareTo(new BigDecimal("0.1")) >= 0 && 
               distanceKm.compareTo(new BigDecimal("500")) <= 0 &&
               durationSeconds != null && 
               durationSeconds >= 120; // At least 2 minutes
    }

    public boolean hasHighSpeed() {
        return avgSpeed != null && avgSpeed.compareTo(new BigDecimal("180")) > 0;
    }

    public boolean hasUnusualEfficiency() {
        if (efficiencyKwhPerKm == null) return false;
        
        BigDecimal efficiencyPer100km = efficiencyKwhPerKm.multiply(new BigDecimal("100"));
        return efficiencyPer100km.compareTo(new BigDecimal("10")) < 0 || 
               efficiencyPer100km.compareTo(new BigDecimal("25")) > 0;
    }

    public String generateQualityReport() {
        StringBuilder report = new StringBuilder();
        
        if (!isValid()) {
            report.append("Invalid trip data: ");
            if (distanceKm == null || distanceKm.compareTo(new BigDecimal("0.1")) < 0) {
                report.append("Distance too short. ");
            }
            if (distanceKm != null && distanceKm.compareTo(new BigDecimal("500")) > 0) {
                report.append("Distance too long. ");
            }
            if (durationSeconds == null || durationSeconds < 120) {
                report.append("Duration too short. ");
            }
        }
        
        if (hasHighSpeed()) {
            report.append("Unusually high speed detected. ");
        }
        
        if (hasUnusualEfficiency()) {
            report.append("Unusual energy efficiency. ");
        }
        
        return report.toString();
    }
}

