package com.carbonmarketplace.verificationservice.service;

import com.carbonmarketplace.verificationservice.dto.request.ReviewVerificationRequest;
import com.carbonmarketplace.verificationservice.dto.request.SubmitVerificationRequest;
import com.carbonmarketplace.verificationservice.dto.response.VerificationResponse;
import com.carbonmarketplace.verificationservice.dto.response.VerificationStatistics;
import com.carbonmarketplace.verificationservice.entity.*;
import com.carbonmarketplace.verificationservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for verification request management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final TripRepository tripRepository;
    private final AnomalyRepository anomalyRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final CreditIssuanceService creditIssuanceService;
    private final AuditService auditService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // SLA configuration (in hours)
    private static final int HIGH_PRIORITY_SLA_HOURS = 24;
    private static final int NORMAL_PRIORITY_SLA_HOURS = 48;
    private static final int LOW_PRIORITY_SLA_HOURS = 72;
    private static final int MAX_REQUESTS_PER_AUDITOR = 10;

    /**
     * Submit a new verification request.
     */
    @Transactional
    public VerificationResponse submitVerificationRequest(SubmitVerificationRequest request) {
        log.info("Submitting verification request for owner {} vehicle {}", 
                request.getOwnerId(), request.getVehicleId());

        // Check for overlapping requests
        List<VerificationRequest> overlapping = verificationRequestRepository.findOverlappingRequests(
                request.getVehicleId(), request.getTripDateStart(), request.getTripDateEnd());
        
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Overlapping verification request already exists");
        }

        // Create verification request
        VerificationRequest verificationRequest = VerificationRequest.builder()
                .ownerId(request.getOwnerId())
                .vehicleId(request.getVehicleId())
                .tripDateStart(request.getTripDateStart())
                .tripDateEnd(request.getTripDateEnd())
                .totalKm(request.getTotalKm())
                .totalTrips(request.getTotalTrips())
                .co2SavedKg(request.getCo2SavedKg())
                .creditAmountTons(request.getCreditAmountTons())
                .methodology(request.getMethodology())
                .calculationDetails(request.getCalculationDetails())
                .dataFileUrl(request.getDataFileUrl())
                .calculationFileUrl(request.getCalculationFileUrl())
                .status(VerificationRequest.VerificationStatus.PENDING)
                .priority(VerificationRequest.Priority.valueOf(request.getPriority()))
                .slaDeadline(calculateSLADeadline(request.getPriority()))
                .build();

        verificationRequest = verificationRequestRepository.save(verificationRequest);
        
        // Load trips for the verification period
        LocalDateTime startDateTime = request.getTripDateStart().atStartOfDay();
        LocalDateTime endDateTime = request.getTripDateEnd().atTime(23, 59, 59);
        List<Trip> trips = tripRepository.findByVehicleAndDateRange(
                request.getVehicleId(), startDateTime, endDateTime);

        // Run anomaly detection
        List<Anomaly> anomalies = anomalyDetectionService.detectAnomalies(
                verificationRequest.getVerificationId(), trips);

        // Auto-assign if no critical anomalies
        boolean hasCriticalAnomalies = anomalies.stream()
                .anyMatch(a -> a.getSeverity() == Anomaly.Severity.CRITICAL);
        
        if (!hasCriticalAnomalies) {
            autoAssignToAuditor(verificationRequest);
        } else {
            log.warn("Critical anomalies detected for verification {}, manual assignment required", 
                    verificationRequest.getVerificationId());
        }

        // Log audit trail
        auditService.logAction(
                verificationRequest.getVerificationId(),
                request.getOwnerId(),
                AuditTrail.AuditAction.CREATE,
                "VerificationRequest",
                verificationRequest.getVerificationId().toString(),
                null,
                "Verification request submitted"
        );

        // Send notification
        sendNotification("verification.submitted", verificationRequest);

        return buildVerificationResponse(verificationRequest, anomalies);
    }

    /**
     * Review and process a verification request.
     */
    @Transactional
    public VerificationResponse reviewVerification(UUID verificationId, UUID auditorId, 
                                                  ReviewVerificationRequest request) {
        log.info("Reviewing verification {} by auditor {}", verificationId, auditorId);

        VerificationRequest verification = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));

        if (verification.getStatus() != VerificationRequest.VerificationStatus.PENDING &&
            verification.getStatus() != VerificationRequest.VerificationStatus.IN_REVIEW) {
            throw new IllegalStateException("Verification is not in reviewable state");
        }

        // Update verification based on action
        String oldStatus = verification.getStatus().toString();
        
        switch (request.getAction()) {
            case APPROVE:
                approveVerification(verification, auditorId, request);
                break;
            case REJECT:
                rejectVerification(verification, auditorId, request);
                break;
            case REQUEST_MORE_INFO:
                requestMoreInfo(verification, auditorId, request);
                break;
            case ADJUST_AMOUNT:
                adjustAmount(verification, auditorId, request);
                break;
        }

        // Process anomaly resolutions if provided
        if (request.getAnomalyResolutions() != null) {
            resolveAnomalies(verificationId, auditorId, request.getAnomalyResolutions());
        }

        // Log audit trail
        auditService.logAction(
                verificationId,
                auditorId,
                AuditTrail.AuditAction.valueOf(request.getAction().toString()),
                "VerificationRequest",
                verificationId.toString(),
                oldStatus,
                request.getReason()
        );

        verification = verificationRequestRepository.save(verification);
        
        // Send notification
        sendNotification("verification.reviewed", verification);

        List<Anomaly> anomalies = anomalyRepository.findByVerificationId(verificationId);
        return buildVerificationResponse(verification, anomalies);
    }

    /**
     * Approve verification and issue credits.
     */
    private void approveVerification(VerificationRequest verification, UUID auditorId, 
                                    ReviewVerificationRequest request) {
        verification.setStatus(VerificationRequest.VerificationStatus.APPROVED);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setApprovedAt(LocalDateTime.now());
        verification.setAuditorNotes(request.getAuditorNotes());
        
        if (request.getCvaOrganization() != null) {
            verification.setCvaOrganization(request.getCvaOrganization());
        }

        // Determine final amount
        BigDecimal finalAmount = verification.getCreditAmountTons();
        if (request.getAdjustedAmountTons() != null) {
            finalAmount = request.getAdjustedAmountTons();
            verification.setAdjustedAmountTons(finalAmount);
        }

        // Issue carbon credits
        CarbonCredit credit = creditIssuanceService.issueCredits(
                verification.getVerificationId(),
                verification.getOwnerId(),
                finalAmount,
                verification.getCvaOrganization()
        );

        log.info("Approved verification {} and issued {} tons of credits with serial {}", 
                verification.getVerificationId(), finalAmount, credit.getSerialNumber());
    }

    /**
     * Reject verification request.
     */
    private void rejectVerification(VerificationRequest verification, UUID auditorId, 
                                   ReviewVerificationRequest request) {
        verification.setStatus(VerificationRequest.VerificationStatus.REJECTED);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setRejectedAt(LocalDateTime.now());
        verification.setRejectionReason(request.getReason());
        verification.setAuditorNotes(request.getAuditorNotes());
        
        log.info("Rejected verification {} with reason: {}", 
                verification.getVerificationId(), request.getReason());
    }

    /**
     * Request more information from the owner.
     */
    private void requestMoreInfo(VerificationRequest verification, UUID auditorId, 
                                ReviewVerificationRequest request) {
        verification.setStatus(VerificationRequest.VerificationStatus.MORE_INFO_REQUIRED);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setAuditorNotes(request.getAuditorNotes());
        
        log.info("Requested more info for verification {}: {}", 
                verification.getVerificationId(), request.getReason());
    }

    /**
     * Adjust credit amount.
     */
    private void adjustAmount(VerificationRequest verification, UUID auditorId, 
                             ReviewVerificationRequest request) {
        if (request.getAdjustedAmountTons() == null) {
            throw new IllegalArgumentException("Adjusted amount is required");
        }
        
        verification.setAdjustedAmountTons(request.getAdjustedAmountTons());
        verification.setAuditorNotes(request.getAuditorNotes());
        
        log.info("Adjusted credit amount for verification {} from {} to {} tons", 
                verification.getVerificationId(), 
                verification.getCreditAmountTons(),
                request.getAdjustedAmountTons());
    }

    /**
     * Resolve anomalies.
     */
    private void resolveAnomalies(UUID verificationId, UUID auditorId, 
                                  List<ReviewVerificationRequest.AnomalyResolution> resolutions) {
        for (ReviewVerificationRequest.AnomalyResolution resolution : resolutions) {
            Anomaly anomaly = anomalyRepository.findById(resolution.getAnomalyId())
                    .orElseThrow(() -> new IllegalArgumentException("Anomaly not found"));
            
            anomaly.setResolutionStatus(Anomaly.ResolutionStatus.valueOf(resolution.getResolutionStatus()));
            anomaly.setResolutionNotes(resolution.getResolutionNotes());
            anomaly.setResolvedBy(auditorId);
            anomaly.setResolvedAt(LocalDateTime.now());
            
            anomalyRepository.save(anomaly);
            
            auditService.logAction(
                    verificationId,
                    auditorId,
                    AuditTrail.AuditAction.RESOLVE_ANOMALY,
                    "Anomaly",
                    anomaly.getAnomalyId().toString(),
                    anomaly.getResolutionStatus().toString(),
                    resolution.getResolutionNotes()
            );
        }
    }

    /**
     * Auto-assign verification request to least loaded auditor.
     */
    private void autoAssignToAuditor(VerificationRequest verification) {
        // Get auditor workload distribution
        List<Object[]> workloads = verificationRequestRepository.getAuditorWorkloadDistribution();
        
        // Find auditor with least workload
        UUID selectedAuditor = null;
        int minWorkload = Integer.MAX_VALUE;
        
        for (Object[] workload : workloads) {
            UUID auditorId = (UUID) workload[0];
            Long count = (Long) workload[1];
            
            if (count < minWorkload && count < MAX_REQUESTS_PER_AUDITOR) {
                selectedAuditor = auditorId;
                minWorkload = count.intValue();
            }
        }
        
        if (selectedAuditor != null) {
            verification.setAssignedAuditorId(selectedAuditor);
            verification.setAssignedAt(LocalDateTime.now());
            verification.setStatus(VerificationRequest.VerificationStatus.IN_REVIEW);
            
            log.info("Auto-assigned verification {} to auditor {} (current workload: {})",
                    verification.getVerificationId(), selectedAuditor, minWorkload);
            
            sendNotification("verification.assigned", verification);
        } else {
            log.warn("No available auditor for auto-assignment of verification {}",
                    verification.getVerificationId());
        }
    }

    /**
     * Calculate SLA deadline based on priority.
     */
    private LocalDateTime calculateSLADeadline(String priority) {
        int hours = switch (priority) {
            case "HIGH" -> HIGH_PRIORITY_SLA_HOURS;
            case "LOW" -> LOW_PRIORITY_SLA_HOURS;
            default -> NORMAL_PRIORITY_SLA_HOURS;
        };
        return LocalDateTime.now().plusHours(hours);
    }

    /**
     * Get verification statistics.
     */
    public VerificationStatistics getStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);
        
        return VerificationStatistics.builder()
                .pendingRequests(verificationRequestRepository.countByStatus(
                        VerificationRequest.VerificationStatus.PENDING))
                .inReviewRequests(verificationRequestRepository.countByStatus(
                        VerificationRequest.VerificationStatus.IN_REVIEW))
                .completedToday(verificationRequestRepository.countApprovedSince(startOfDay))
                .completedThisWeek(verificationRequestRepository.countApprovedSince(startOfWeek))
                .completedThisMonth(verificationRequestRepository.countApprovedSince(startOfMonth))
                .averageReviewTimeHours(verificationRequestRepository.getAverageReviewTimeHours(startOfMonth))
                .overdueRequests(verificationRequestRepository.findOverdueRequests(
                        VerificationRequest.VerificationStatus.PENDING, now).size())
                .totalCreditsIssuedTons(BigDecimal.valueOf(
                        verificationRequestRepository.getTotalCreditsApproved(startOfMonth)))
                .statisticsGeneratedAt(now)
                .build();
    }

    /**
     * Check for overdue verifications (scheduled task).
     */
    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void checkOverdueVerifications() {
        LocalDateTime now = LocalDateTime.now();
        List<VerificationRequest> overdueRequests = verificationRequestRepository.findOverdueRequests(
                VerificationRequest.VerificationStatus.PENDING, now);
        
        for (VerificationRequest request : overdueRequests) {
            log.warn("Verification {} is overdue (deadline was {})",
                    request.getVerificationId(), request.getSlaDeadline());
            
            // Escalate to high priority
            request.setPriority(VerificationRequest.Priority.HIGH);
            verificationRequestRepository.save(request);
            
            // Send alert
            sendNotification("verification.overdue", request);
        }
    }

    /**
     * Build verification response DTO.
     */
    private VerificationResponse buildVerificationResponse(VerificationRequest verification,
                                                          List<Anomaly> anomalies) {
        VerificationResponse response = VerificationResponse.builder()
                .verificationId(verification.getVerificationId())
                .ownerId(verification.getOwnerId())
                .vehicleId(verification.getVehicleId())
                .tripDateStart(verification.getTripDateStart())
                .tripDateEnd(verification.getTripDateEnd())
                .totalKm(verification.getTotalKm())
                .totalTrips(verification.getTotalTrips())
                .co2SavedKg(verification.getCo2SavedKg())
                .creditAmountTons(verification.getCreditAmountTons())
                .adjustedAmountTons(verification.getAdjustedAmountTons())
                .methodology(verification.getMethodology())
                .status(verification.getStatus())
                .priority(verification.getPriority())
                .assignedAuditorId(verification.getAssignedAuditorId())
                .assignedAt(verification.getAssignedAt())
                .cvaOrganization(verification.getCvaOrganization())
                .auditorNotes(verification.getAuditorNotes())
                .rejectionReason(verification.getRejectionReason())
                .dataFileUrl(verification.getDataFileUrl())
                .calculationFileUrl(verification.getCalculationFileUrl())
                .slaDeadline(verification.getSlaDeadline())
                .createdAt(verification.getCreatedAt())
                .reviewedAt(verification.getReviewedAt())
                .approvedAt(verification.getApprovedAt())
                .rejectedAt(verification.getRejectedAt())
                .updatedAt(verification.getUpdatedAt())
                .build();
        
        // Add anomaly information
        if (anomalies != null && !anomalies.isEmpty()) {
            response.setTotalAnomalies(anomalies.size());
            response.setCriticalAnomalies((int) anomalies.stream()
                    .filter(a -> a.getSeverity() == Anomaly.Severity.CRITICAL)
                    .count());
            response.setAnomalies(anomalies.stream()
                    .map(this::mapAnomalyToInfo)
                    .collect(Collectors.toList()));
        }
        
        // Calculate remaining time
        response.setHoursRemaining(response.calculateHoursRemaining());
        response.setIsOverdue(response.checkIsOverdue());
        
        return response;
    }

    /**
     * Map anomaly to info DTO.
     */
    private VerificationResponse.AnomalyInfo mapAnomalyToInfo(Anomaly anomaly) {
        return VerificationResponse.AnomalyInfo.builder()
                .anomalyId(anomaly.getAnomalyId())
                .anomalyType(anomaly.getAnomalyType().toString())
                .severity(anomaly.getSeverity().toString())
                .description(anomaly.getDescription())
                .resolutionStatus(anomaly.getResolutionStatus().toString())
                .tripId(anomaly.getTripId())
                .build();
    }

    /**
     * Send notification via Kafka.
     */
    private void sendNotification(String topic, Object payload) {
        try {
            kafkaTemplate.send(topic, payload);
        } catch (Exception e) {
            log.error("Failed to send notification to topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Get verification details.
     */
    public VerificationResponse getVerificationDetails(UUID verificationId) {
        VerificationRequest verification = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        
        List<Anomaly> anomalies = anomalyRepository.findByVerificationId(verificationId);
        return buildVerificationResponse(verification, anomalies);
    }

    /**
     * Assign verification to specific auditor.
     */
    @Transactional
    public VerificationResponse assignVerificationToAuditor(UUID verificationId, UUID auditorId) {
        VerificationRequest verification = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        
        verification.setAssignedAuditorId(auditorId);
        verification.setAssignedAt(LocalDateTime.now());
        verification.setStatus(VerificationRequest.VerificationStatus.IN_REVIEW);
        
        verification = verificationRequestRepository.save(verification);
        sendNotification("verification.assigned", verification);
        
        List<Anomaly> anomalies = anomalyRepository.findByVerificationId(verificationId);
        return buildVerificationResponse(verification, anomalies);
    }

    /**
     * Verify audit trail integrity.
     */
    public boolean verifyAuditTrailIntegrity(UUID verificationId) {
        return auditService.verifyHashChain(verificationId);
    }

    /**
     * Generate verification report.
     */
    public byte[] generateVerificationReport(UUID verificationId) {
        // This would integrate with a PDF generation library like iText or Apache PDFBox
        // For now, return a placeholder
        String report = auditService.generateAuditReport(verificationId, 
                LocalDateTime.now().minusYears(1), LocalDateTime.now());
        return report.getBytes();
    }
}
