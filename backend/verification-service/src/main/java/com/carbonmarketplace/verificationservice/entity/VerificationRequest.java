package com.carbonmarketplace.verificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a carbon credit verification request.
 */
@Entity
@Table(name = "verification_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "verification_id")
    private UUID verificationId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    // Request Scope
    @Column(name = "trip_date_start", nullable = false)
    private LocalDate tripDateStart;

    @Column(name = "trip_date_end", nullable = false)
    private LocalDate tripDateEnd;

    @Column(name = "total_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalKm;

    @Column(name = "total_trips", nullable = false)
    private Integer totalTrips;

    // Carbon Calculation
    @Column(name = "co2_saved_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal co2SavedKg;

    @Column(name = "credit_amount_tons", nullable = false, precision = 10, scale = 4)
    private BigDecimal creditAmountTons;

    @Column(name = "methodology")
    private String methodology = "CDM ACM0018";

    @Column(name = "calculation_details", columnDefinition = "jsonb")
    private String calculationDetails;

    // Verification Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VerificationStatus status = VerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority = Priority.NORMAL;

    // CVA Assignment
    @Column(name = "assigned_auditor_id")
    private UUID assignedAuditorId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "cva_organization")
    private String cvaOrganization;

    // Auditor Review
    @Column(name = "auditor_notes", columnDefinition = "TEXT")
    private String auditorNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "adjusted_amount_tons", precision = 10, scale = 4)
    private BigDecimal adjustedAmountTons;

    // Supporting Documents
    @Column(name = "data_file_url")
    private String dataFileUrl;

    @Column(name = "calculation_file_url")
    private String calculationFileUrl;

    // SLA Tracking
    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    // Timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum VerificationStatus {
        PENDING,
        IN_REVIEW,
        APPROVED,
        REJECTED,
        MORE_INFO_REQUIRED
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH
    }
}
