package com.carbonmarketplace.verificationservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for submitting a new verification request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitVerificationRequest {

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;

    @NotNull(message = "Trip date start is required")
    @PastOrPresent(message = "Trip date start cannot be in the future")
    private LocalDate tripDateStart;

    @NotNull(message = "Trip date end is required")
    @PastOrPresent(message = "Trip date end cannot be in the future")
    private LocalDate tripDateEnd;

    @NotNull(message = "Total kilometers is required")
    @DecimalMin(value = "1.0", message = "Total kilometers must be at least 1.0")
    @DecimalMax(value = "100000.0", message = "Total kilometers cannot exceed 100,000")
    private BigDecimal totalKm;

    @NotNull(message = "Total trips is required")
    @Min(value = 1, message = "Total trips must be at least 1")
    @Max(value = 10000, message = "Total trips cannot exceed 10,000")
    private Integer totalTrips;

    @NotNull(message = "CO2 saved is required")
    @DecimalMin(value = "0.01", message = "CO2 saved must be at least 0.01 kg")
    private BigDecimal co2SavedKg;

    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "0.0001", message = "Credit amount must be at least 0.0001 tons")
    private BigDecimal creditAmountTons;

    private String methodology = "CDM ACM0018";

    private String calculationDetails;

    private String dataFileUrl;

    private String calculationFileUrl;

    @Size(max = 100, message = "Region cannot exceed 100 characters")
    private String region;

    private String priority = "NORMAL";
}
