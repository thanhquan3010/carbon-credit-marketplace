package com.carbonmarketplace.verificationservice.dto.response;

import com.carbonmarketplace.verificationservice.entity.VerificationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for verification request details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    private UUID verificationId;
    private UUID ownerId;
    private String ownerName;
    private UUID vehicleId;
    private String vehicleInfo;

    // Request Scope
    private LocalDate tripDateStart;
    private LocalDate tripDateEnd;
    private BigDecimal totalKm;
    private Integer totalTrips;

    // Carbon Calculation
    private BigDecimal co2SavedKg;
    private BigDecimal creditAmountTons;
    private BigDecimal adjustedAmountTons;
    private String methodology;
    private Object calculationDetails;

    // Status
    private VerificationRequest.VerificationStatus status;
    private VerificationRequest.Priority priority;

    // Assignment
    private UUID assignedAuditorId;
    private String assignedAuditorName;
    private LocalDateTime assignedAt;
    private String cvaOrganization;

    // Review
    private String auditorNotes;
    private String rejectionReason;

    // Documents
    private String dataFileUrl;
    private String calculationFileUrl;

    // SLA
    private LocalDateTime slaDeadline;
    private Long hoursRemaining;
    private Boolean isOverdue;

    // Anomalies
    private List<AnomalyInfo> anomalies;
    private Integer totalAnomalies;
    private Integer criticalAnomalies;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime updatedAt;

    // Data Quality
    private BigDecimal dataQualityScore;
    private String dataSource;

    // Issued Credits (if approved)
    private String creditSerialNumber;
    private LocalDateTime creditIssuedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyInfo {
        private UUID anomalyId;
        private String anomalyType;
        private String severity;
        private String description;
        private String resolutionStatus;
        private UUID tripId;
    }

    /**
     * Calculate hours remaining until SLA deadline.
     */
    public Long calculateHoursRemaining() {
        if (slaDeadline == null) {
            return null;
        }
        long hours = java.time.Duration.between(LocalDateTime.now(), slaDeadline).toHours();
        return Math.max(0, hours);
    }

    /**
     * Check if verification is overdue.
     */
    public Boolean checkIsOverdue() {
        if (slaDeadline == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(slaDeadline);
    }
}
