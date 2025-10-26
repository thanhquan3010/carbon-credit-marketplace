package com.carbonmarketplace.verificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing detected anomalies in trip data.
 */
@Entity
@Table(name = "verification_anomalies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "anomaly_id")
    private UUID anomalyId;

    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "detected_value")
    private String detectedValue;

    @Column(name = "expected_value")
    private String expectedValue;

    @Column(name = "deviation_percentage")
    private Double deviationPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status")
    private ResolutionStatus resolutionStatus = ResolutionStatus.UNRESOLVED;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreatedDate
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    // Enums
    public enum AnomalyType {
        HIGH_SPEED,
        LOW_SPEED,
        DUPLICATE_TRIP,
        EFFICIENCY_OUTLIER,
        DISTANCE_MISMATCH,
        TIME_OVERLAP,
        GPS_JUMP,
        ENERGY_CONSUMPTION_OUTLIER,
        INVALID_GPS_COORDINATES,
        MISSING_DATA
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ResolutionStatus {
        UNRESOLVED,
        UNDER_REVIEW,
        RESOLVED,
        IGNORED,
        ESCALATED
    }
}
