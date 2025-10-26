package com.carbonmarketplace.vehicleservice.dto.response;

import com.carbonmarketplace.vehicleservice.entity.Vehicle;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for vehicle information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private UUID vehicleId;
    private UUID ownerId;

    // Vehicle Information
    private String make;
    private String model;
    private Integer year;
    private String vin;
    private String registrationNumber;
    private String color;

    // Data Source
    private Vehicle.DataSource dataSource;
    private Vehicle.SyncFrequency syncFrequency;

    // Verification
    private Vehicle.VerificationStatus verificationStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime verifiedAt;

    // Sync Status
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextSyncAt;
    
    private Vehicle.SyncStatus syncStatus;

    // Statistics
    private Double totalDistanceKm;
    private Integer totalTrips;
    private Double totalCo2SavedKg;

    // Status
    private Boolean isActive;
    private String notes;

    // Audit
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Convert Vehicle entity to VehicleResponse DTO
     */
    public static VehicleResponse fromEntity(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .ownerId(vehicle.getOwnerId())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .vin(vehicle.getVin())
                .registrationNumber(vehicle.getRegistrationNumber())
                .color(vehicle.getColor())
                .dataSource(vehicle.getDataSource())
                .syncFrequency(vehicle.getSyncFrequency())
                .verificationStatus(vehicle.getVerificationStatus())
                .verifiedAt(vehicle.getVerifiedAt())
                .lastSyncAt(vehicle.getLastSyncAt())
                .nextSyncAt(vehicle.getNextSyncAt())
                .syncStatus(vehicle.getSyncStatus())
                .totalDistanceKm(vehicle.getTotalDistanceKm())
                .totalTrips(vehicle.getTotalTrips())
                .totalCo2SavedKg(vehicle.getTotalCo2SavedKg())
                .isActive(vehicle.getIsActive())
                .notes(vehicle.getNotes())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}
