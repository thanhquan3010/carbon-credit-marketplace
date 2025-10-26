package com.carbonmarketplace.carboncreditservice.dto.request;

import com.carbonmarketplace.carboncreditservice.entity.VerificationRequest.Priority;
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
 * Request DTO for creating a verification request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequestDto {
    
    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;
    
    @NotNull(message = "Trip start date is required")
    @PastOrPresent(message = "Trip start date cannot be in the future")
    private LocalDate tripDateStart;
    
    @NotNull(message = "Trip end date is required")
    @PastOrPresent(message = "Trip end date cannot be in the future")
    private LocalDate tripDateEnd;
    
    @NotNull(message = "Total distance is required")
    @Positive(message = "Total distance must be positive")
    private BigDecimal totalKm;
    
    @NotNull(message = "Total trips is required")
    @Positive(message = "Total trips must be positive")
    private Integer totalTrips;
    
    @NotNull(message = "CO2 saved is required")
    @Positive(message = "CO2 saved must be positive")
    private BigDecimal co2SavedKg;
    
    @NotNull(message = "Credit amount is required")
    @Positive(message = "Credit amount must be positive")
    private BigDecimal creditAmountTons;
    
    private String methodology = "CDM ACM0018";
    
    private Priority priority = Priority.NORMAL;
    
    private String notes;
    
    private List<UUID> tripIds;
    
    private String dataFileUrl;
    
    private String calculationFileUrl;
}
