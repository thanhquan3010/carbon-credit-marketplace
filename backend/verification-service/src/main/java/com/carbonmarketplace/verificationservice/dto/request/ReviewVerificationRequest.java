package com.carbonmarketplace.verificationservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for reviewing a verification request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVerificationRequest {

    @NotNull(message = "Action is required")
    private ReviewAction action;

    @Size(max = 5000, message = "Auditor notes cannot exceed 5000 characters")
    private String auditorNotes;

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    @DecimalMin(value = "0.0001", message = "Adjusted amount must be at least 0.0001 tons")
    private BigDecimal adjustedAmountTons;

    @Size(max = 255, message = "CVA organization cannot exceed 255 characters")
    private String cvaOrganization;

    private List<AnomalyResolution> anomalyResolutions;

    public enum ReviewAction {
        APPROVE,
        REJECT,
        REQUEST_MORE_INFO,
        ADJUST_AMOUNT
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyResolution {
        
        @NotNull(message = "Anomaly ID is required")
        private UUID anomalyId;

        @NotNull(message = "Resolution status is required")
        private String resolutionStatus;

        @Size(max = 1000, message = "Resolution notes cannot exceed 1000 characters")
        private String resolutionNotes;
    }
}
