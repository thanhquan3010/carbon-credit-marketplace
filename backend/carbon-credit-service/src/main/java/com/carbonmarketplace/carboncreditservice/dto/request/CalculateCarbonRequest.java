package com.carbonmarketplace.carboncreditservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for calculating carbon credits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateCarbonRequest {
    
    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;
    
    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDate endDate;
    
    private List<UUID> tripIds;  // Optional: specific trips to include
    
    @Positive(message = "Total distance must be positive")
    private BigDecimal totalDistanceKm;  // Optional: if trips not provided
    
    private String methodology = "CDM ACM0018";
    
    private Boolean includeAnomalousTrips = false;
}
