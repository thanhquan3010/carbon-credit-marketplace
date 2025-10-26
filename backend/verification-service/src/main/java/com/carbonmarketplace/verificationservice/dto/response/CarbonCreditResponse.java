package com.carbonmarketplace.verificationservice.dto.response;

import com.carbonmarketplace.verificationservice.entity.CarbonCredit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for carbon credit details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonCreditResponse {

    private UUID creditId;
    private String serialNumber;

    // Ownership
    private UUID ownerId;
    private String ownerName;
    private UUID originalOwnerId;
    private String originalOwnerName;

    // Credit Details
    private BigDecimal amountTons;
    private Integer vintageYear;
    private String methodology;
    private String region;

    // Verification
    private UUID verificationId;
    private String cvaOrganization;
    private LocalDateTime verifiedAt;
    private String verifiedBy;

    // Status
    private CarbonCredit.CreditStatus status;

    // Market Info
    private UUID listingId;
    private UUID transactionId;
    private BigDecimal marketValue;

    // Retirement
    private Boolean isRetired;
    private LocalDateTime retiredAt;
    private String retirementReason;

    // Certificate
    private String certificateUrl;
    private String certificateNumber;
    private String qrCode;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Environmental Impact
    private String environmentalEquivalent;
    
    /**
     * Calculate environmental equivalent (e.g., trees planted).
     */
    public String calculateEnvironmentalEquivalent() {
        if (amountTons == null) {
            return null;
        }
        // 1 ton CO2 ≈ 16 trees
        BigDecimal trees = amountTons.multiply(new BigDecimal("16"));
        return String.format("Equivalent to %s trees planted", trees.setScale(0, BigDecimal.ROUND_HALF_UP));
    }
}
