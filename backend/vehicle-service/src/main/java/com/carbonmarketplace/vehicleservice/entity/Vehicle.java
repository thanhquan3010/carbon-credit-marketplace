package com.carbonmarketplace.vehicleservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an electric vehicle registered by EV owners
 */
@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicle_owner", columnList = "owner_id"),
        @Index(name = "idx_vehicle_vin", columnList = "vin", unique = true),
        @Index(name = "idx_vehicle_status", columnList = "verification_status"),
        @Index(name = "idx_vehicle_sync", columnList = "next_sync_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"owner"})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vehicle_id", updatable = false, nullable = false)
    private UUID vehicleId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Vehicle Information
    @Column(name = "make", nullable = false, length = 100)
    private String make;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "vin", nullable = false, unique = true, length = 17)
    private String vin;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "color", length = 50)
    private String color;

    // Data Source Configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 50)
    private DataSource dataSource = DataSource.MANUAL;

    @Column(name = "data_source_config", columnDefinition = "TEXT")
    private String dataSourceConfig; // Store as JSON string

    // Verification
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    // Sync Status
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_frequency", length = 50)
    private SyncFrequency syncFrequency = SyncFrequency.DAILY;

    @Column(name = "next_sync_at")
    private LocalDateTime nextSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", length = 50)
    private SyncStatus syncStatus = SyncStatus.SUCCESS;

    @Column(name = "total_distance_km", columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private Double totalDistanceKm = 0.0;

    @Column(name = "total_trips")
    private Integer totalTrips = 0;

    @Column(name = "total_co2_saved_kg", columnDefinition = "DECIMAL(10,4) DEFAULT 0")
    private Double totalCo2SavedKg = 0.0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
        MANUAL("manual");

        private final String value;

        DataSource(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum VerificationStatus {
        PENDING("pending"),
        VERIFIED("verified"),
        REJECTED("rejected");

        private final String value;

        VerificationStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SyncFrequency {
        HOURLY("hourly"),
        DAILY("daily"),
        WEEKLY("weekly"),
        MONTHLY("monthly");

        private final String value;

        SyncFrequency(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SyncStatus {
        SUCCESS("success"),
        FAILED("failed"),
        IN_PROGRESS("in_progress");

        private final String value;

        SyncStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Helper methods
    @PrePersist
    public void prePersist() {
        if (syncFrequency != null) {
            calculateNextSyncTime();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (syncFrequency != null && lastSyncAt != null) {
            calculateNextSyncTime();
        }
    }

    public void calculateNextSyncTime() {
        LocalDateTime baseTime = lastSyncAt != null ? lastSyncAt : LocalDateTime.now();
        
        switch (syncFrequency) {
            case HOURLY:
                nextSyncAt = baseTime.plusHours(1);
                break;
            case DAILY:
                nextSyncAt = baseTime.plusDays(1).withHour(2).withMinute(0).withSecond(0);
                break;
            case WEEKLY:
                nextSyncAt = baseTime.plusWeeks(1);
                break;
            case MONTHLY:
                nextSyncAt = baseTime.plusMonths(1);
                break;
            default:
                nextSyncAt = baseTime.plusDays(1);
        }
    }

    public boolean needsSync() {
        return nextSyncAt != null && LocalDateTime.now().isAfter(nextSyncAt);
    }

    public void markAsSynced() {
        this.lastSyncAt = LocalDateTime.now();
        this.syncStatus = SyncStatus.SUCCESS;
        calculateNextSyncTime();
    }

    public void markSyncFailed() {
        this.syncStatus = SyncStatus.FAILED;
    }

    public void markSyncInProgress() {
        this.syncStatus = SyncStatus.IN_PROGRESS;
    }
}

