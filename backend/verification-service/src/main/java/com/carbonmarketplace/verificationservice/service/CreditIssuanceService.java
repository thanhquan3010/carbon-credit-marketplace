package com.carbonmarketplace.verificationservice.service;

import com.carbonmarketplace.verificationservice.entity.CarbonCredit;
import com.carbonmarketplace.verificationservice.entity.VerificationRequest;
import com.carbonmarketplace.verificationservice.repository.CarbonCreditRepository;
import com.carbonmarketplace.verificationservice.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for issuing carbon credits with unique serial numbers.
 * Serial number format: VN-EV-YYYY-CVA-SEQUENCE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditIssuanceService {

    private final CarbonCreditRepository carbonCreditRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final AuditService auditService;
    
    // Lock for thread-safe serial number generation
    private final Lock serialNumberLock = new ReentrantLock();
    
    // CVA organization codes
    private static final String DEFAULT_CVA = "VCS"; // Verified Carbon Standard
    
    /**
     * Issue carbon credits for an approved verification.
     */
    @Transactional
    public CarbonCredit issueCredits(UUID verificationId, UUID ownerId, 
                                     BigDecimal amountTons, String cvaOrganization) {
        log.info("Issuing {} tons of carbon credits for verification {}", amountTons, verificationId);
        
        // Validate verification exists and is approved
        VerificationRequest verification = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        
        if (verification.getStatus() != VerificationRequest.VerificationStatus.APPROVED) {
            throw new IllegalStateException("Verification must be approved before issuing credits");
        }
        
        // Check if credits already issued for this verification
        if (!carbonCreditRepository.findByVerificationId(verificationId).isEmpty()) {
            throw new IllegalStateException("Credits already issued for this verification");
        }
        
        // Generate unique serial number
        String serialNumber = generateSerialNumber(cvaOrganization);
        
        // Create carbon credit
        CarbonCredit credit = CarbonCredit.builder()
                .serialNumber(serialNumber)
                .ownerId(ownerId)
                .originalOwnerId(ownerId)
                .amountTons(amountTons)
                .vintageYear(Year.now().getValue())
                .methodology(verification.getMethodology())
                .region(extractRegion(verification))
                .verificationId(verificationId)
                .cvaOrganization(cvaOrganization != null ? cvaOrganization : DEFAULT_CVA)
                .verifiedAt(LocalDateTime.now())
                .status(CarbonCredit.CreditStatus.ISSUED)
                .build();
        
        credit = carbonCreditRepository.save(credit);
        
        // Log issuance in audit trail
        auditService.logAction(
                verificationId,
                ownerId,
                com.carbonmarketplace.verificationservice.entity.AuditTrail.AuditAction.ISSUE_CREDITS,
                "CarbonCredit",
                credit.getCreditId().toString(),
                null,
                String.format("Issued %s tons of carbon credits with serial number %s", 
                        amountTons, serialNumber)
        );
        
        log.info("Successfully issued carbon credit {} with serial number {}", 
                credit.getCreditId(), serialNumber);
        
        return credit;
    }
    
    /**
     * Generate unique serial number.
     * Format: VN-EV-YYYY-CVA-SEQUENCE
     * Example: VN-EV-2025-VCS-000001234
     */
    private String generateSerialNumber(String cvaOrganization) {
        serialNumberLock.lock();
        try {
            int currentYear = Year.now().getValue();
            String cvaCode = mapCvaToCode(cvaOrganization);
            String prefix = String.format("VN-EV-%d-%s-", currentYear, cvaCode);
            
            // Get the maximum sequence number for this prefix
            Long maxSequence = carbonCreditRepository.getMaxSerialNumberSequence(prefix);
            long nextSequence = (maxSequence != null ? maxSequence : 0) + 1;
            
            // Format with leading zeros (9 digits)
            String serialNumber = String.format("%s%09d", prefix, nextSequence);
            
            log.debug("Generated serial number: {}", serialNumber);
            return serialNumber;
        } finally {
            serialNumberLock.unlock();
        }
    }
    
    /**
     * Map CVA organization name to code.
     */
    private String mapCvaToCode(String cvaOrganization) {
        if (cvaOrganization == null || cvaOrganization.isEmpty()) {
            return DEFAULT_CVA;
        }
        
        // Common CVA organization mappings
        return switch (cvaOrganization.toUpperCase()) {
            case "VERIFIED CARBON STANDARD", "VCS" -> "VCS";
            case "GOLD STANDARD", "GS" -> "GS";
            case "CLIMATE ACTION RESERVE", "CAR" -> "CAR";
            case "AMERICAN CARBON REGISTRY", "ACR" -> "ACR";
            case "PURO EARTH", "PURO" -> "PURO";
            case "TÜV RHEINLAND", "TUV" -> "TUV";
            case "BUREAU VERITAS", "BV" -> "BV";
            case "DNV GL", "DNV" -> "DNV";
            case "SGS" -> "SGS";
            case "RINA" -> "RINA";
            default -> {
                // Use first 3 characters of organization name
                String code = cvaOrganization.replaceAll("[^A-Z]", "").toUpperCase();
                yield code.length() >= 3 ? code.substring(0, 3) : DEFAULT_CVA;
            }
        };
    }
    
    /**
     * Extract region from verification request.
     */
    private String extractRegion(VerificationRequest verification) {
        // This could be enhanced to extract from vehicle data or user profile
        // For now, return a default region
        return "HCM"; // Ho Chi Minh City
    }
    
    /**
     * Transfer carbon credits to new owner.
     */
    @Transactional
    public CarbonCredit transferCredits(UUID creditId, UUID fromOwnerId, UUID toOwnerId) {
        log.info("Transferring credit {} from owner {} to {}", creditId, fromOwnerId, toOwnerId);
        
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found"));
        
        // Validate current owner
        if (!credit.getOwnerId().equals(fromOwnerId)) {
            throw new IllegalStateException("Credit is not owned by the specified owner");
        }
        
        // Validate credit status
        if (credit.getStatus() != CarbonCredit.CreditStatus.ISSUED &&
            credit.getStatus() != CarbonCredit.CreditStatus.LISTED) {
            throw new IllegalStateException("Credit cannot be transferred in current status");
        }
        
        // Perform transfer
        String oldOwnerId = credit.getOwnerId().toString();
        credit.setOwnerId(toOwnerId);
        credit.setStatus(CarbonCredit.CreditStatus.ISSUED);
        credit = carbonCreditRepository.save(credit);
        
        // Log transfer in audit trail
        auditService.logAction(
                credit.getVerificationId(),
                toOwnerId,
                com.carbonmarketplace.verificationservice.entity.AuditTrail.AuditAction.UPDATE,
                "CarbonCredit",
                creditId.toString(),
                oldOwnerId,
                String.format("Transferred credit from %s to %s", fromOwnerId, toOwnerId)
        );
        
        log.info("Successfully transferred credit {}", creditId);
        return credit;
    }
    
    /**
     * Retire carbon credits.
     */
    @Transactional
    public CarbonCredit retireCredits(UUID creditId, UUID ownerId, String reason) {
        log.info("Retiring credit {} for owner {}", creditId, ownerId);
        
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found"));
        
        // Validate owner
        if (!credit.getOwnerId().equals(ownerId)) {
            throw new IllegalStateException("Credit is not owned by the specified owner");
        }
        
        // Validate credit status
        if (credit.getStatus() == CarbonCredit.CreditStatus.RETIRED) {
            throw new IllegalStateException("Credit is already retired");
        }
        
        // Retire credit
        credit.setStatus(CarbonCredit.CreditStatus.RETIRED);
        credit.setRetiredAt(LocalDateTime.now());
        credit.setRetirementReason(reason);
        credit = carbonCreditRepository.save(credit);
        
        // Log retirement in audit trail
        auditService.logAction(
                credit.getVerificationId(),
                ownerId,
                com.carbonmarketplace.verificationservice.entity.AuditTrail.AuditAction.UPDATE,
                "CarbonCredit",
                creditId.toString(),
                "ISSUED/SOLD",
                String.format("Retired credit: %s", reason)
        );
        
        log.info("Successfully retired credit {} with reason: {}", creditId, reason);
        return credit;
    }
    
    /**
     * Get total credits issued for an owner.
     */
    public BigDecimal getTotalCreditsForOwner(UUID ownerId) {
        BigDecimal total = carbonCreditRepository.getTotalCreditsByOwnerAndStatus(
                ownerId, CarbonCredit.CreditStatus.ISSUED);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Get total credits issued since a date.
     */
    public BigDecimal getTotalCreditsIssuedSince(LocalDateTime since) {
        BigDecimal total = carbonCreditRepository.getTotalCreditsIssuedSince(since);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Validate credit authenticity by serial number.
     */
    public boolean validateCreditAuthenticity(String serialNumber) {
        return carbonCreditRepository.findBySerialNumber(serialNumber).isPresent();
    }
}
