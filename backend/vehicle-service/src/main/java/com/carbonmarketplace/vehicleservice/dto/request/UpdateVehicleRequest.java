package com.carbonmarketplace.vehicleservice.dto.request;

import com.carbonmarketplace.vehicleservice.entity.Vehicle;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating vehicle information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVehicleRequest {

    @Size(max = 100, message = "Make must not exceed 100 characters")
    private String make;

    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @Min(value = 2010, message = "Year must be 2010 or later")
    @Max(value = 2100, message = "Year must be reasonable")
    private Integer year;

    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Invalid VIN format")
    private String vin;

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    @Size(max = 50, message = "Color must not exceed 50 characters")
    private String color;

    private Vehicle.DataSource dataSource;

    private String dataSourceConfig;

    private Vehicle.SyncFrequency syncFrequency;

    private Boolean isActive;

    private String notes;
}

