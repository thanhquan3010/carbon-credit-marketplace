package com.carbonmarketplace.verificationservice.dto.response;

import com.carbonmarketplace.verificationservice.entity.Anomaly;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for anomaly details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyResponse {

    private UUID anomalyId;
    private UUID verificationId;
    private UUID tripId;

    // Anomaly Details
    private Anomaly.AnomalyType anomalyType;
    private Anomaly.Severity severity;
    private String description;
    private String detectedValue;
    private String expectedValue;
    private Double deviationPercentage;

    // Trip Information
    private LocalDateTime tripDate;
    private String tripRoute;
    private Double tripDistance;

    // Resolution
    private Anomaly.ResolutionStatus resolutionStatus;
    private String resolutionNotes;
    private UUID resolvedBy;
    private String resolvedByName;
    private LocalDateTime resolvedAt;

    // Detection
    private LocalDateTime detectedAt;
    private String detectionMethod;

    // Impact
    private String impactOnVerification;
    private Boolean requiresManualReview;

    /**
     * Get severity color for UI display.
     */
    public String getSeverityColor() {
        if (severity == null) {
            return "#808080"; // Gray
        }
        return switch (severity) {
            case LOW -> "#28a745"; // Green
            case MEDIUM -> "#ffc107"; // Yellow
            case HIGH -> "#fd7e14"; // Orange
            case CRITICAL -> "#dc3545"; // Red
        };
    }

    /**
     * Check if anomaly is resolved.
     */
    public Boolean isResolved() {
        return resolutionStatus == Anomaly.ResolutionStatus.RESOLVED ||
               resolutionStatus == Anomaly.ResolutionStatus.IGNORED;
    }
}
