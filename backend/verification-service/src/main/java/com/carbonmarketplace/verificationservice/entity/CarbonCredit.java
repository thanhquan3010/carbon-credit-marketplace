package com.carbonmarketplace.verificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing issued carbon credits.
 */
@Entity
@Table(name = "carbon_credits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CarbonCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "credit_id")
    private UUID creditId;

    @Column(name = "serial_number", unique = true, nullable = false)
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

    @Column(name = "methodology", nullable = false)
    private String methodology;

    @Column(name = "region")
    private String region;

    // Verification
    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(name = "cva_organization", nullable = false)
    private String cvaOrganization;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    // Status Tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CreditStatus status = CreditStatus.PENDING;

    // Listing Link (if listed)
    @Column(name = "listing_id")
    private UUID listingId;

    // Transaction History
    @Column(name = "transaction_id")
    private UUID transactionId;

    // Retirement
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "retirement_reason")
    private String retirementReason;

    // Metadata
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum
    public enum CreditStatus {
        PENDING,
        ISSUED,
        LISTED,
        SOLD,
        RETIRED
    }
}
