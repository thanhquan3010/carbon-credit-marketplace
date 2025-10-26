package com.carbonmarketplace.carboncreditservice.service;

import com.carbonmarketplace.carboncreditservice.dto.CarbonCalculationResult;
import com.carbonmarketplace.carboncreditservice.dto.request.VerificationRequestDto;
import com.carbonmarketplace.carboncreditservice.dto.response.CarbonCreditResponse;
import com.carbonmarketplace.carboncreditservice.dto.response.VerificationResponse;
import com.carbonmarketplace.carboncreditservice.entity.*;
import com.carbonmarketplace.carboncreditservice.exception.VerificationException;
import com.carbonmarketplace.carboncreditservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling carbon credit verification requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {
    
    private final VerificationRequestRepository verificationRepository;
    private final CarbonCreditRepository creditRepository;
    private final TripRepository tripRepository;
    private final CarbonWalletService walletService;
    private final CarbonCalculationService calculationService;
    
    @Value("${carbon.verification.max-pending-requests:5}")
    private int maxPendingRequests;
    
    @Value("${carbon.verification.min-trips:10}")
    private int minTripsRequired;
    
    @Value("${carbon.verification.min-distance-km:100}")
    private BigDecimal minDistanceKm;
    
    /**
     * Submit a new verification request
     */
    @Transactional
    public VerificationResponse submitVerificationRequest(UUID userId, VerificationRequestDto request) {
        log.info("Submitting verification request for user {} vehicle {}", userId, request.getVehicleId());
        
        // Validate request
        validateVerificationRequest(userId, request);
        
        // Check for overlapping pending requests
        List<VerificationRequest> overlapping = verificationRepository.findOverlappingPendingRequests(
                request.getVehicleId(),
                request.getTripDateStart(),
                request.getTripDateEnd()
        );
        
        if (!overlapping.isEmpty()) {
            throw new VerificationException("Overlapping verification request already exists for this period");
        }
        
        // Check pending request limit
        Long pendingCount = verificationRepository.countPendingRequestsByOwner(userId);
        if (pendingCount >= maxPendingRequests) {
            throw new VerificationException(
                    "Maximum pending requests reached. Please wait for existing requests to be processed."
            );
        }
        
        // Link trips if provided
        List<Trip> trips = new ArrayList<>();
        if (request.getTripIds() != null && !request.getTripIds().isEmpty()) {
            trips = tripRepository.findByTripIdIn(request.getTripIds());
            
            // Validate trip ownership
            for (Trip trip : trips) {
                if (!trip.getVehicleId().equals(request.getVehicleId())) {
                    throw new IllegalArgumentException("Trip " + trip.getTripId() + " doesn't belong to vehicle");
                }
            }
        }
        
        // Recalculate if trips are provided
        CarbonCalculationResult calculationResult = null;
        if (!trips.isEmpty()) {
            calculationResult = calculationService.calculate(trips);
            
            // Validate calculation matches request
            if (!calculationService.validateAccuracy(calculationResult.getCreditAmountTons(), request.getCreditAmountTons())) {
                log.warn("Recalculated amount {} differs from requested amount {}",
                        calculationResult.getCreditAmountTons(), request.getCreditAmountTons());
            }
        }
        
        // Create verification request
        VerificationRequest verification = VerificationRequest.builder()
                .ownerId(userId)
                .vehicleId(request.getVehicleId())
                .tripDateStart(request.getTripDateStart())
                .tripDateEnd(request.getTripDateEnd())
                .totalKm(request.getTotalKm())
                .totalTrips(request.getTotalTrips())
                .co2SavedKg(request.getCo2SavedKg())
                .creditAmountTons(request.getCreditAmountTons())
                .methodology(request.getMethodology())
                .priority(request.getPriority())
                .status(VerificationRequest.VerificationStatus.PENDING)
                .dataFileUrl(request.getDataFileUrl())
                .calculationFileUrl(request.getCalculationFileUrl())
                .trips(trips)
                .build();
        
        // Store calculation details if available
        if (calculationResult != null && calculationResult.getDetails() != null) {
            verification.setCalculationDetails(convertToJson(calculationResult.getDetails()));
        }
        
        verification = verificationRepository.save(verification);
        
        // Update trips with verification reference
        for (Trip trip : trips) {
            trip.setVerificationRequest(verification);
            tripRepository.save(trip);
        }
        
        log.info("Verification request {} submitted successfully", verification.getVerificationId());
        
        return mapToVerificationResponse(verification);
    }
    
    /**
     * Get verification request details
     */
    @Transactional(readOnly = true)
    public VerificationResponse getVerificationRequest(UUID verificationId) {
        VerificationRequest verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationException("Verification request not found: " + verificationId));
        
        return mapToVerificationResponse(verification);
    }
    
    /**
     * Get user's verification requests
     */
    @Transactional(readOnly = true)
    public Page<VerificationResponse> getUserVerificationRequests(UUID userId, Pageable pageable) {
        Page<VerificationRequest> requests = verificationRepository.findByOwnerId(userId, pageable);
        return requests.map(this::mapToVerificationResponse);
    }
    
    /**
     * Approve verification request (for auditors)
     */
    @Transactional
    public VerificationResponse approveVerification(UUID verificationId, UUID auditorId, String notes) {
        log.info("Approving verification request {} by auditor {}", verificationId, auditorId);
        
        VerificationRequest verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationException("Verification request not found"));
        
        if (verification.getStatus() != VerificationRequest.VerificationStatus.IN_REVIEW) {
            throw new IllegalStateException("Can only approve requests in IN_REVIEW status");
        }
        
        // Update verification status
        verification.setStatus(VerificationRequest.VerificationStatus.APPROVED);
        verification.setAuditorNotes(notes);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setApprovedAt(LocalDateTime.now());
        
        verification = verificationRepository.save(verification);
        
        // Issue carbon credits
        List<CarbonCredit> issuedCredits = issueCredits(verification);
        
        // Add credits to wallet
        BigDecimal totalCredits = issuedCredits.stream()
                .map(CarbonCredit::getAmountTons)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        walletService.addVerifiedCredits(
                verification.getOwnerId(),
                totalCredits,
                verification.getVerificationId()
        );
        
        log.info("Verification approved. {} credits issued totaling {} tons",
                issuedCredits.size(), totalCredits);
        
        return mapToVerificationResponse(verification);
    }
    
    /**
     * Reject verification request
     */
    @Transactional
    public VerificationResponse rejectVerification(UUID verificationId, UUID auditorId, String reason) {
        log.info("Rejecting verification request {} by auditor {}", verificationId, auditorId);
        
        VerificationRequest verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationException("Verification request not found"));
        
        if (verification.getStatus() != VerificationRequest.VerificationStatus.IN_REVIEW) {
            throw new IllegalStateException("Can only reject requests in IN_REVIEW status");
        }
        
        verification.setStatus(VerificationRequest.VerificationStatus.REJECTED);
        verification.setRejectionReason(reason);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setRejectedAt(LocalDateTime.now());
        
        verification = verificationRepository.save(verification);
        
        log.info("Verification request rejected. Reason: {}", reason);
        
        return mapToVerificationResponse(verification);
    }
    
    /**
     * Assign verification request to auditor
     */
    @Transactional
    public VerificationResponse assignToAuditor(UUID verificationId, UUID auditorId, String cvaOrganization) {
        log.info("Assigning verification {} to auditor {} from {}", verificationId, auditorId, cvaOrganization);
        
        VerificationRequest verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationException("Verification request not found"));
        
        if (verification.getStatus() != VerificationRequest.VerificationStatus.PENDING) {
            throw new IllegalStateException("Can only assign pending requests");
        }
        
        // Check auditor workload
        Long activeAssignments = verificationRepository.countActiveAssignments(auditorId);
        if (activeAssignments >= 10) { // Max 10 active assignments per auditor
            throw new VerificationException("Auditor has too many active assignments");
        }
        
        verification.setAssignedAuditorId(auditorId);
        verification.setAssignedAt(LocalDateTime.now());
        verification.setCvaOrganization(cvaOrganization);
        verification.setStatus(VerificationRequest.VerificationStatus.IN_REVIEW);
        
        verification = verificationRepository.save(verification);
        
        log.info("Verification assigned successfully");
        
        return mapToVerificationResponse(verification);
    }
    
    /**
     * Issue carbon credits for approved verification
     */
    private List<CarbonCredit> issueCredits(VerificationRequest verification) {
        List<CarbonCredit> credits = new ArrayList<>();
        
        // Determine how to split credits (e.g., monthly batches)
        BigDecimal totalAmount = verification.getAdjustedAmountTons() != null ?
                verification.getAdjustedAmountTons() : verification.getCreditAmountTons();
        
        // For now, issue as a single credit
        // In production, might split into smaller denominations
        int sequenceNumber = getNextSequenceNumber(verification.getCvaOrganization());
        
        CarbonCredit credit = CarbonCredit.builder()
                .serialNumber(CarbonCredit.generateSerialNumber(
                        verification.getCvaOrganization(),
                        sequenceNumber
                ))
                .ownerId(verification.getOwnerId())
                .originalOwnerId(verification.getOwnerId())
                .amountTons(totalAmount)
                .vintageYear(LocalDateTime.now().getYear())
                .methodology(verification.getMethodology())
                .region("Vietnam") // Could be parameterized
                .verificationRequest(verification)
                .cvaOrganization(verification.getCvaOrganization())
                .verifiedAt(LocalDateTime.now())
                .status(CarbonCredit.CreditStatus.ISSUED)
                .build();
        
        credit = creditRepository.save(credit);
        credits.add(credit);
        
        log.info("Issued credit {} for {} tons", credit.getSerialNumber(), credit.getAmountTons());
        
        return credits;
    }
    
    /**
     * Get next sequence number for serial number generation
     */
    private int getNextSequenceNumber(String cvaOrganization) {
        String prefix = String.format("VN-EV-%d-%s",
                LocalDateTime.now().getYear(),
                cvaOrganization.substring(0, Math.min(3, cvaOrganization.length())).toUpperCase()
        );
        
        Integer maxSequence = creditRepository.findMaxSequenceNumberForPrefix(prefix);
        return (maxSequence != null ? maxSequence : 0) + 1;
    }
    
    /**
     * Get credits issued from verification
     */
    @Transactional(readOnly = true)
    public Page<CarbonCreditResponse> getVerificationCredits(UUID verificationId, Pageable pageable) {
        Page<CarbonCredit> credits = creditRepository.findByVerificationRequestVerificationId(
                verificationId, pageable
        );
        
        return credits.map(this::mapToCreditResponse);
    }
    
    /**
     * Validate verification request
     */
    private void validateVerificationRequest(UUID userId, VerificationRequestDto request) {
        // Check minimum requirements
        if (request.getTotalTrips() < minTripsRequired) {
            throw new VerificationException(
                    "Minimum " + minTripsRequired + " trips required for verification"
            );
        }
        
        if (request.getTotalKm().compareTo(minDistanceKm) < 0) {
            throw new VerificationException(
                    "Minimum distance of " + minDistanceKm + " km required for verification"
            );
        }
        
        // Check date range
        if (request.getTripDateEnd().isBefore(request.getTripDateStart())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        // Validate calculations
        BigDecimal expectedCO2 = calculationService.quickCalculate(request.getTotalKm()).getCo2SavedKg();
        if (!calculationService.validateAccuracy(request.getCo2SavedKg(), expectedCO2)) {
            throw new VerificationException("CO2 calculation does not match expected value");
        }
    }
    
    /**
     * Convert object to JSON string
     */
    private String convertToJson(Object obj) {
        // In production, use Jackson ObjectMapper
        return obj.toString();
    }
    
    /**
     * Map verification entity to response DTO
     */
    private VerificationResponse mapToVerificationResponse(VerificationRequest verification) {
        return VerificationResponse.builder()
                .verificationId(verification.getVerificationId())
                .ownerId(verification.getOwnerId())
                .vehicleId(verification.getVehicleId())
                .tripDateStart(verification.getTripDateStart())
                .tripDateEnd(verification.getTripDateEnd())
                .totalKm(verification.getTotalKm())
                .totalTrips(verification.getTotalTrips())
                .co2SavedKg(verification.getCo2SavedKg())
                .creditAmountTons(verification.getCreditAmountTons())
                .methodology(verification.getMethodology())
                .status(verification.getStatus())
                .priority(verification.getPriority())
                .assignedAuditorId(verification.getAssignedAuditorId())
                .assignedAt(verification.getAssignedAt())
                .cvaOrganization(verification.getCvaOrganization())
                .auditorNotes(verification.getAuditorNotes())
                .rejectionReason(verification.getRejectionReason())
                .adjustedAmountTons(verification.getAdjustedAmountTons())
                .dataFileUrl(verification.getDataFileUrl())
                .calculationFileUrl(verification.getCalculationFileUrl())
                .slaDeadline(verification.getSlaDeadline())
                .createdAt(verification.getCreatedAt())
                .reviewedAt(verification.getReviewedAt())
                .approvedAt(verification.getApprovedAt())
                .rejectedAt(verification.getRejectedAt())
                .updatedAt(verification.getUpdatedAt())
                .build();
    }
    
    /**
     * Map credit entity to response DTO
     */
    private CarbonCreditResponse mapToCreditResponse(CarbonCredit credit) {
        return CarbonCreditResponse.builder()
                .creditId(credit.getCreditId())
                .serialNumber(credit.getSerialNumber())
                .ownerId(credit.getOwnerId())
                .originalOwnerId(credit.getOriginalOwnerId())
                .amountTons(credit.getAmountTons())
                .vintageYear(credit.getVintageYear())
                .methodology(credit.getMethodology())
                .region(credit.getRegion())
                .status(credit.getStatus())
                .cvaOrganization(credit.getCvaOrganization())
                .verifiedAt(credit.getVerifiedAt())
                .verificationId(credit.getVerificationRequest().getVerificationId())
                .createdAt(credit.getCreatedAt())
                .updatedAt(credit.getUpdatedAt())
                .isRetired(credit.getRetiredAt() != null)
                .retiredAt(credit.getRetiredAt())
                .retirementReason(credit.getRetirementReason())
                .build();
    }
}
