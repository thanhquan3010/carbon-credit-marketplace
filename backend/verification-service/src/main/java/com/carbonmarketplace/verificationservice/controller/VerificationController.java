package com.carbonmarketplace.verificationservice.controller;

import com.carbonmarketplace.verificationservice.dto.request.ReviewVerificationRequest;
import com.carbonmarketplace.verificationservice.dto.request.SubmitVerificationRequest;
import com.carbonmarketplace.verificationservice.dto.response.VerificationResponse;
import com.carbonmarketplace.verificationservice.dto.response.VerificationStatistics;
import com.carbonmarketplace.verificationservice.entity.VerificationRequest;
import com.carbonmarketplace.verificationservice.repository.VerificationRequestRepository;
import com.carbonmarketplace.verificationservice.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for verification operations.
 */
@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verification", description = "Carbon credit verification management")
@SecurityRequirement(name = "bearerAuth")
public class VerificationController {

    private final VerificationService verificationService;
    private final VerificationRequestRepository verificationRequestRepository;

    /**
     * Submit a new verification request.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EV_OWNER', 'ADMIN')")
    @Operation(summary = "Submit verification request", 
               description = "Submit a new carbon credit verification request")
    public ResponseEntity<VerificationResponse> submitVerification(
            @Valid @RequestBody SubmitVerificationRequest request,
            Authentication authentication) {
        
        log.info("Received verification submission from user {}", authentication.getName());
        
        // Set owner ID from authentication if not provided
        if (request.getOwnerId() == null) {
            request.setOwnerId(UUID.fromString(authentication.getName()));
        }
        
        VerificationResponse response = verificationService.submitVerificationRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get verification by ID.
     */
    @GetMapping("/{verificationId}")
    @PreAuthorize("hasAnyRole('EV_OWNER', 'CVA_AUDITOR', 'ADMIN')")
    @Operation(summary = "Get verification details", 
               description = "Get detailed information about a verification request")
    public ResponseEntity<VerificationResponse> getVerification(
            @PathVariable UUID verificationId,
            Authentication authentication) {
        
        VerificationRequest verification = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        
        // Check access permission
        if (!hasAccessToVerification(verification, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        VerificationResponse response = verificationService.getVerificationDetails(verificationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Review verification request (for auditors).
     */
    @PutMapping("/{verificationId}/review")
    @PreAuthorize("hasAnyRole('CVA_AUDITOR', 'ADMIN')")
    @Operation(summary = "Review verification", 
               description = "Review and process a verification request")
    public ResponseEntity<VerificationResponse> reviewVerification(
            @PathVariable UUID verificationId,
            @Valid @RequestBody ReviewVerificationRequest request,
            Authentication authentication) {
        
        UUID auditorId = UUID.fromString(authentication.getName());
        log.info("Auditor {} reviewing verification {}", auditorId, verificationId);
        
        VerificationResponse response = verificationService.reviewVerification(
                verificationId, auditorId, request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get verifications for current user.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('EV_OWNER')")
    @Operation(summary = "Get my verifications", 
               description = "Get all verification requests for the current user")
    public ResponseEntity<Page<VerificationResponse>> getMyVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            Authentication authentication) {
        
        UUID ownerId = UUID.fromString(authentication.getName());
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<VerificationRequest> verifications = verificationRequestRepository.findByOwnerId(
                ownerId, pageable);
        
        Page<VerificationResponse> responses = verifications.map(v -> 
                verificationService.getVerificationDetails(v.getVerificationId()));
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get verification queue for auditors.
     */
    @GetMapping("/queue")
    @PreAuthorize("hasRole('CVA_AUDITOR')")
    @Operation(summary = "Get verification queue", 
               description = "Get pending verifications for auditor review")
    public ResponseEntity<Page<VerificationResponse>> getVerificationQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            Authentication authentication) {
        
        UUID auditorId = UUID.fromString(authentication.getName());
        
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.ASC, "slaDeadline"));
        
        Page<VerificationRequest> verifications;
        
        if (status != null && priority != null) {
            verifications = verificationRequestRepository.findByStatusAndPriority(
                    VerificationRequest.VerificationStatus.valueOf(status),
                    VerificationRequest.Priority.valueOf(priority),
                    pageable);
        } else if (status != null) {
            verifications = verificationRequestRepository.findByStatus(
                    VerificationRequest.VerificationStatus.valueOf(status),
                    pageable);
        } else {
            verifications = verificationRequestRepository.findByAssignedAuditorId(
                    auditorId, pageable);
        }
        
        Page<VerificationResponse> responses = verifications.map(v -> 
                verificationService.getVerificationDetails(v.getVerificationId()));
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get verification statistics.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('CVA_AUDITOR', 'ADMIN')")
    @Operation(summary = "Get statistics", 
               description = "Get verification statistics and metrics")
    public ResponseEntity<VerificationStatistics> getStatistics() {
        VerificationStatistics statistics = verificationService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Assign verification to auditor.
     */
    @PostMapping("/{verificationId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign verification", 
               description = "Manually assign verification to an auditor")
    public ResponseEntity<VerificationResponse> assignVerification(
            @PathVariable UUID verificationId,
            @RequestParam UUID auditorId,
            Authentication authentication) {
        
        log.info("Admin {} assigning verification {} to auditor {}", 
                authentication.getName(), verificationId, auditorId);
        
        VerificationResponse response = verificationService.assignVerificationToAuditor(
                verificationId, auditorId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Verify audit trail integrity.
     */
    @GetMapping("/{verificationId}/verify-audit")
    @PreAuthorize("hasAnyRole('CVA_AUDITOR', 'ADMIN')")
    @Operation(summary = "Verify audit trail", 
               description = "Verify the integrity of the audit trail hash chain")
    public ResponseEntity<Map<String, Object>> verifyAuditTrail(
            @PathVariable UUID verificationId) {
        
        boolean isValid = verificationService.verifyAuditTrailIntegrity(verificationId);
        
        return ResponseEntity.ok(Map.of(
                "verificationId", verificationId,
                "hashChainValid", isValid,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Download verification report.
     */
    @GetMapping("/{verificationId}/report")
    @PreAuthorize("hasAnyRole('EV_OWNER', 'CVA_AUDITOR', 'ADMIN')")
    @Operation(summary = "Download report", 
               description = "Download verification report as PDF")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable UUID verificationId,
            Authentication authentication) {
        
        VerificationRequest verification = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found"));
        
        if (!hasAccessToVerification(verification, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        byte[] reportData = verificationService.generateVerificationReport(verificationId);
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", 
                        "attachment; filename=verification-" + verificationId + ".pdf")
                .body(reportData);
    }

    /**
     * Check if user has access to verification.
     */
    private boolean hasAccessToVerification(VerificationRequest verification, 
                                           Authentication authentication) {
        String userId = authentication.getName();
        boolean isOwner = verification.getOwnerId().toString().equals(userId);
        boolean isAssignedAuditor = verification.getAssignedAuditorId() != null &&
                verification.getAssignedAuditorId().toString().equals(userId);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        return isOwner || isAssignedAuditor || isAdmin;
    }
}
