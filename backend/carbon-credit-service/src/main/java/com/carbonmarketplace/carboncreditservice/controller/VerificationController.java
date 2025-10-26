package com.carbonmarketplace.carboncreditservice.controller;

import com.carbonmarketplace.carboncreditservice.dto.request.VerificationRequestDto;
import com.carbonmarketplace.carboncreditservice.dto.response.CarbonCreditResponse;
import com.carbonmarketplace.carboncreditservice.dto.response.VerificationResponse;
import com.carbonmarketplace.carboncreditservice.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.UUID;

/**
 * REST controller for carbon credit verification requests
 */
@RestController
@RequestMapping("/api/v1/carbon/verification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verification", description = "Carbon credit verification endpoints")
@SecurityRequirement(name = "bearerAuth")
public class VerificationController {
    
    private final VerificationService verificationService;
    
    @Operation(summary = "Submit verification request",
            description = "Submit a new carbon credit verification request")
    @PostMapping
    @PreAuthorize("hasRole('EVOWNER')")
    public ResponseEntity<VerificationResponse> submitVerification(
            Authentication authentication,
            @Valid @RequestBody VerificationRequestDto request) {
        
        UUID userId = getUserIdFromAuth(authentication);
        
        log.info("User {} submitting verification request for vehicle {}",
                userId, request.getVehicleId());
        
        VerificationResponse response = verificationService.submitVerificationRequest(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "Get verification request",
            description = "Get details of a specific verification request")
    @GetMapping("/{verificationId}")
    @PreAuthorize("hasAnyRole('EVOWNER', 'VERIFIER', 'ADMIN')")
    public ResponseEntity<VerificationResponse> getVerification(
            @PathVariable UUID verificationId) {
        
        VerificationResponse response = verificationService.getVerificationRequest(verificationId);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get my verification requests",
            description = "Get all verification requests for the authenticated user")
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('EVOWNER')")
    public ResponseEntity<Page<VerificationResponse>> getMyVerifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        UUID userId = getUserIdFromAuth(authentication);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<VerificationResponse> requests = verificationService.getUserVerificationRequests(userId, pageable);
        
        return ResponseEntity.ok(requests);
    }
    
    @Operation(summary = "Approve verification",
            description = "Approve a verification request and issue credits (auditor only)")
    @PostMapping("/{verificationId}/approve")
    @PreAuthorize("hasRole('VERIFIER')")
    public ResponseEntity<VerificationResponse> approveVerification(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @RequestParam(required = false) String notes) {
        
        UUID auditorId = getUserIdFromAuth(authentication);
        
        log.info("Auditor {} approving verification {}", auditorId, verificationId);
        
        VerificationResponse response = verificationService.approveVerification(
                verificationId, auditorId, notes
        );
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Reject verification",
            description = "Reject a verification request (auditor only)")
    @PostMapping("/{verificationId}/reject")
    @PreAuthorize("hasRole('VERIFIER')")
    public ResponseEntity<VerificationResponse> rejectVerification(
            Authentication authentication,
            @PathVariable UUID verificationId,
            @RequestParam String reason) {
        
        UUID auditorId = getUserIdFromAuth(authentication);
        
        log.info("Auditor {} rejecting verification {}", auditorId, verificationId);
        
        VerificationResponse response = verificationService.rejectVerification(
                verificationId, auditorId, reason
        );
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Assign verification to auditor",
            description = "Assign a verification request to an auditor (admin only)")
    @PostMapping("/{verificationId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VerificationResponse> assignVerification(
            @PathVariable UUID verificationId,
            @RequestParam UUID auditorId,
            @RequestParam String cvaOrganization) {
        
        log.info("Assigning verification {} to auditor {} from {}",
                verificationId, auditorId, cvaOrganization);
        
        VerificationResponse response = verificationService.assignToAuditor(
                verificationId, auditorId, cvaOrganization
        );
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get credits from verification",
            description = "Get carbon credits issued from a verification request")
    @GetMapping("/{verificationId}/credits")
    @PreAuthorize("hasAnyRole('EVOWNER', 'VERIFIER', 'ADMIN')")
    public ResponseEntity<Page<CarbonCreditResponse>> getVerificationCredits(
            @PathVariable UUID verificationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<CarbonCreditResponse> credits = verificationService.getVerificationCredits(
                verificationId, pageable
        );
        
        return ResponseEntity.ok(credits);
    }
    
    /**
     * Extract user ID from authentication
     */
    private UUID getUserIdFromAuth(Authentication authentication) {
        // In a real implementation, this would extract the user ID from the JWT token
        // For now, using a placeholder
        return UUID.fromString(authentication.getName());
    }
}
