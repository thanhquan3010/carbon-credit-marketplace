package com.carbonmarketplace.carboncreditservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for communicating with Vehicle Service
 */
@FeignClient(name = "vehicle-service", url = "${feign.client.config.vehicle-service.url}")
public interface VehicleServiceClient {
    
    @GetMapping("/api/v1/vehicles/{vehicleId}")
    VehicleDto getVehicleById(@PathVariable UUID vehicleId);
    
    @GetMapping("/api/v1/vehicles/{vehicleId}/owner")
    UUID getVehicleOwnerId(@PathVariable UUID vehicleId);
    
    @GetMapping("/api/v1/vehicles/{vehicleId}/verification-status")
    VehicleVerificationStatusDto getVehicleVerificationStatus(@PathVariable UUID vehicleId);
}

/**
 * Vehicle DTO
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class VehicleDto {
    private UUID vehicleId;
    private UUID ownerId;
    private String make;
    private String model;
    private Integer year;
    private String vin;
    private String registrationNumber;
    private String verificationStatus;
}

/**
 * Vehicle Verification Status DTO
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class VehicleVerificationStatusDto {
    private UUID vehicleId;
    private String status;
    private Boolean isVerified;
}
