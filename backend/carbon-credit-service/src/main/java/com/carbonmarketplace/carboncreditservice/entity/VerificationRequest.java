package com.carbonmarketplace.carboncreditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a carbon credit verification request submitted to CVA
 */
@Entity
@Table(name = "verification_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "verification_id", updatable = false, nullable = false)
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
    
    @Column(name = "methodology", length = 100)
    @Builder.Default
    private String methodology = "CDM ACM0018";
    
    @Column(name = "calculation_details", columnDefinition = "jsonb")
    private String calculationDetails;
    
    // Verification Status
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus status = VerificationStatus.PENDING;
    
    @Column(name = "priority", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.NORMAL;
    
    // CVA Assignment
    @Column(name = "assigned_auditor_id")
    private UUID assignedAuditorId;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "cva_organization", length = 255)
    private String cvaOrganization;
    
    // Auditor Review
    @Column(name = "auditor_notes", length = 1000)
    private String auditorNotes;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    @Column(name = "adjusted_amount_tons", precision = 10, scale = 4)
    private BigDecimal adjustedAmountTons;
    
    // Supporting Documents
    @Column(name = "data_file_url", length = 500)
    private String dataFileUrl;
    
    @Column(name = "calculation_file_url", length = 500)
    private String calculationFileUrl;
    
    // SLA Tracking
    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;
    
    // Relationships
    @OneToMany(mappedBy = "verificationRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Trip> trips = new ArrayList<>();
    
    @OneToMany(mappedBy = "verificationRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<CarbonCredit> carbonCredits = new ArrayList<>();
    
    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum VerificationStatus {
        PENDING,
        IN_REVIEW,
        APPROVED,
        REJECTED,
        MORE_INFO_REQUIRED
    }
    
    public enum Priority {
        LOW, NORMAL, HIGH
    }
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = VerificationStatus.PENDING;
        }
        if (priority == null) {
            priority = Priority.NORMAL;
        }
        if (methodology == null) {
            methodology = "CDM ACM0018";
        }
        
        // Set SLA deadline (7 days for normal priority)
        if (slaDeadline == null) {
            switch (priority) {
                case HIGH:
                    slaDeadline = now.plusDays(3);
                    break;
                case LOW:
                    slaDeadline = now.plusDays(14);
                    break;
                case NORMAL:
                default:
                    slaDeadline = now.plusDays(7);
            }
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
