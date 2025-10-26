package com.carbonmarketplace.vehicleservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for uploading trip data manually or via CSV
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripUploadRequest {

    private String externalTripId;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    // Location (optional)
    @DecimalMin(value = "-90.0", message = "Invalid latitude")
    @DecimalMax(value = "90.0", message = "Invalid latitude")
    private BigDecimal startLat;

    @DecimalMin(value = "-180.0", message = "Invalid longitude")
    @DecimalMax(value = "180.0", message = "Invalid longitude")
    private BigDecimal startLng;

    @DecimalMin(value = "-90.0", message = "Invalid latitude")
    @DecimalMax(value = "90.0", message = "Invalid latitude")
    private BigDecimal endLat;

    @DecimalMin(value = "-180.0", message = "Invalid longitude")
    @DecimalMax(value = "180.0", message = "Invalid longitude")
    private BigDecimal endLng;

    private String startAddress;
    private String endAddress;

    // Distance & Energy
    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.1", message = "Distance must be at least 0.1 km")
    @DecimalMax(value = "500", message = "Distance cannot exceed 500 km")
    private BigDecimal distanceKm;

    @DecimalMin(value = "0", message = "Speed cannot be negative")
    @DecimalMax(value = "180", message = "Speed cannot exceed 180 km/h")
    private BigDecimal avgSpeed;

    @DecimalMin(value = "0", message = "Speed cannot be negative")
    @DecimalMax(value = "200", message = "Max speed cannot exceed 200 km/h")
    private BigDecimal maxSpeed;

    @DecimalMin(value = "0", message = "Energy consumed cannot be negative")
    private BigDecimal energyConsumedKwh;

    // Additional metrics (optional)
    private Integer idleTimeSeconds;
    private Integer drivingTimeSeconds;
    private Integer chargingTimeSeconds;
    private BigDecimal regenerationKwh;
    private Integer ambientTemperature;
    private Integer batteryLevelStart;
    private Integer batteryLevelEnd;

    // Validation
    public boolean isValidDuration() {
        if (startTime == null || endTime == null) return false;
        return endTime.isAfter(startTime) && 
               java.time.Duration.between(startTime, endTime).toMinutes() >= 2;
    }
}
