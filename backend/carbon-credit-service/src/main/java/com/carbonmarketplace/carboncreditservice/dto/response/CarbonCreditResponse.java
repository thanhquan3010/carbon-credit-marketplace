package com.carbonmarketplace.carboncreditservice.dto.response;

import com.carbonmarketplace.carboncreditservice.entity.CarbonCredit.CreditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for carbon credit details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonCreditResponse {
    
    private UUID creditId;
    private String serialNumber;
    private UUID ownerId;
    private UUID originalOwnerId;
    private BigDecimal amountTons;
    private Integer vintageYear;
    private String methodology;
    private String region;
    private CreditStatus status;
    private String cvaOrganization;
    private LocalDateTime verifiedAt;
    private UUID verificationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isRetired;
    private LocalDateTime retiredAt;
    private String retirementReason;
}
