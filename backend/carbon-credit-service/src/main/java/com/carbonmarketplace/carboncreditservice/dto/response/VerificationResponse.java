package com.carbonmarketplace.carboncreditservice.dto.response;

import com.carbonmarketplace.carboncreditservice.entity.VerificationRequest.Priority;
import com.carbonmarketplace.carboncreditservice.entity.VerificationRequest.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for verification request details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    
    private UUID verificationId;
    private UUID ownerId;
    private UUID vehicleId;
    private LocalDate tripDateStart;
    private LocalDate tripDateEnd;
    private BigDecimal totalKm;
    private Integer totalTrips;
    private BigDecimal co2SavedKg;
    private BigDecimal creditAmountTons;
    private String methodology;
    private VerificationStatus status;
    private Priority priority;
    private UUID assignedAuditorId;
    private LocalDateTime assignedAt;
    private String cvaOrganization;
    private String auditorNotes;
    private String rejectionReason;
    private BigDecimal adjustedAmountTons;
    private String dataFileUrl;
    private String calculationFileUrl;
    private LocalDateTime slaDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime updatedAt;
}
