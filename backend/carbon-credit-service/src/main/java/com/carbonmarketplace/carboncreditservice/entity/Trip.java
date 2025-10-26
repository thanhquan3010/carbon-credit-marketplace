package com.carbonmarketplace.carboncreditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an EV trip for carbon credit calculation
 */
@Entity
@Table(name = "trips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trip_id", updatable = false, nullable = false)
    private UUID tripId;
    
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(name = "duration_seconds", insertable = false, updatable = false)
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
    
    @Column(name = "start_address", length = 500)
    private String startAddress;
    
    @Column(name = "end_address", length = 500)
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
    @Column(name = "data_source", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DataSource dataSource;
    
    @Column(name = "data_quality_score", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal dataQualityScore = BigDecimal.ONE;
    
    @Column(name = "has_anomaly")
    @Builder.Default
    private Boolean hasAnomaly = false;
    
    @Column(name = "anomaly_description", length = 500)
    private String anomalyDescription;
    
    // Sync Information
    @Column(name = "sync_batch_id")
    private UUID syncBatchId;
    
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;
    
    // Verification Link
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private VerificationRequest verificationRequest;
    
    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum DataSource {
        API, OBD, MANUAL
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (dataQualityScore == null) {
            dataQualityScore = BigDecimal.ONE;
        }
        if (hasAnomaly == null) {
            hasAnomaly = false;
        }
    }
}
