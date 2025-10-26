package com.carbonmarketplace.vehicleservice.dto.request;

import com.carbonmarketplace.vehicleservice.entity.Vehicle;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vehicle registration request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistrationRequest {

    @NotBlank(message = "Make is required")
    @Size(max = 100, message = "Make must not exceed 100 characters")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 2010, message = "Year must be 2010 or later")
    @Max(value = 2100, message = "Year must be reasonable")
    private Integer year;

    @NotBlank(message = "VIN is required")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Invalid VIN format")
    private String vin;

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    @Size(max = 50, message = "Color must not exceed 50 characters")
    private String color;

    @NotNull(message = "Data source is required")
    private Vehicle.DataSource dataSource;

    private String dataSourceConfig; // JSON string with API credentials or OBD settings

    private Vehicle.SyncFrequency syncFrequency = Vehicle.SyncFrequency.DAILY;

    private String notes;
}
