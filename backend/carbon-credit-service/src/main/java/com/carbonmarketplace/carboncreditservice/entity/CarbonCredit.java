package com.carbonmarketplace.carboncreditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing issued carbon credits available for trading
 */
@Entity
@Table(name = "carbon_credits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonCredit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credit_id", updatable = false, nullable = false)
    private UUID creditId;
    
    @Column(name = "serial_number", unique = true, nullable = false, length = 100)
    private String serialNumber;
    
    // Ownership
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    
    @Column(name = "original_owner_id", nullable = false)
    private UUID originalOwnerId;
    
    // Credit Details
    @Column(name = "amount_tons", nullable = false, precision = 10, scale = 4)
    private BigDecimal amountTons;
    
    @Column(name = "vintage_year", nullable = false)
    private Integer vintageYear;
    
    @Column(name = "methodology", nullable = false, length = 100)
    private String methodology;
    
    @Column(name = "region", length = 100)
    private String region;
    
    // Verification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    @ToString.Exclude
    private VerificationRequest verificationRequest;
    
    @Column(name = "cva_organization", nullable = false, length = 255)
    private String cvaOrganization;
    
    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;
    
    // Status Tracking
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CreditStatus status = CreditStatus.PENDING;
    
    // Listing Link (if listed)
    @Column(name = "listing_id")
    private UUID listingId;
    
    // Transaction History
    @Column(name = "transaction_id")
    private UUID transactionId;
    
    @OneToMany(mappedBy = "carbonCredit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<CreditTransaction> creditTransactions = new ArrayList<>();
    
    // Retirement
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;
    
    @Column(name = "retirement_reason", length = 500)
    private String retirementReason;
    
    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum CreditStatus {
        PENDING,
        ISSUED,
        LISTED,
        SOLD,
        RETIRED
    }
    
    /**
     * Generate a unique serial number for the carbon credit
     * Format: VN-EV-{year}-{auditor}-{sequence}
     * Example: VN-EV-2025-TUV-000012345
     */
    public static String generateSerialNumber(String auditor, int sequence) {
        int year = LocalDateTime.now().getYear();
        String auditorCode = auditor.toUpperCase().substring(0, Math.min(auditor.length(), 3));
        return String.format("VN-EV-%d-%s-%09d", year, auditorCode, sequence);
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
            status = CreditStatus.PENDING;
        }
        if (vintageYear == null) {
            vintageYear = now.getYear();
        }
        
        // Set original owner if not set
        if (originalOwnerId == null && ownerId != null) {
            originalOwnerId = ownerId;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if the credit is available for trading
     */
    public boolean isAvailable() {
        return status == CreditStatus.ISSUED && 
               retiredAt == null && 
               listingId == null;
    }
    
    /**
     * Transfer ownership to a new owner
     */
    public void transferOwnership(UUID newOwnerId) {
        if (newOwnerId == null) {
            throw new IllegalArgumentException("New owner ID cannot be null");
        }
        this.ownerId = newOwnerId;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Retire the credit
     */
    public void retire(String reason) {
        if (status == CreditStatus.RETIRED) {
            throw new IllegalStateException("Credit is already retired");
        }
        this.status = CreditStatus.RETIRED;
        this.retiredAt = LocalDateTime.now();
        this.retirementReason = reason;
        this.updatedAt = LocalDateTime.now();
    }
}
