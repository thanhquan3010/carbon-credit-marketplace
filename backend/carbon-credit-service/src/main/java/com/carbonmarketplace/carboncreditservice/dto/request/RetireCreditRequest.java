package com.carbonmarketplace.carboncreditservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for retiring carbon credits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetireCreditRequest {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amountTons;
    
    @NotBlank(message = "Retirement reason is required")
    private String retirementReason;
    
    private List<UUID> creditIds;  // Optional: specific credits to retire
    
    private String beneficiary;  // Optional: entity benefiting from retirement
    
    private String projectDescription;  // Optional: description of offsetting project
}
