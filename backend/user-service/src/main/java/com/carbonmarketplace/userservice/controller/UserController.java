package com.carbonmarketplace.userservice.controller;

import com.carbonmarketplace.userservice.dto.request.KycDocumentRequest;
import com.carbonmarketplace.userservice.dto.request.UpdateUserRequest;
import com.carbonmarketplace.userservice.dto.response.ApiResponse;
import com.carbonmarketplace.userservice.dto.response.UserResponse;
import com.carbonmarketplace.userservice.entity.KycDocument;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.service.KycService;
import com.carbonmarketplace.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and account management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final KycService kycService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieve the authenticated user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        log.info("Get current user request for: {}", currentUser.getUserId());

        UserResponse userResponse = UserResponse.fromEntity(currentUser);

        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update user profile", description = "Update the authenticated user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserRequest request) {

        log.info("Update profile request for user: {}", currentUser.getUserId());

        UserResponse updatedUser = userService.updateUser(currentUser.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }

    @PostMapping(value = "/me/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit KYC documents", description = "Upload KYC verification documents")
    public ResponseEntity<ApiResponse<KycDocument>> submitKyc(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("kycLevel") Integer kycLevel,
            @RequestParam("idType") KycDocument.IdType idType,
            @RequestParam("idNumber") String idNumber,
            @RequestParam("idFront") MultipartFile idFrontFile,
            @RequestParam(value = "idBack", required = false) MultipartFile idBackFile,
            @RequestParam("selfie") MultipartFile selfieFile,
            @RequestParam(value = "addressProof", required = false) MultipartFile addressProofFile,
            @RequestParam(value = "businessLicense", required = false) MultipartFile businessLicenseFile) {

        log.info("KYC submission request for user: {}", currentUser.getUserId());

        KycDocumentRequest request = KycDocumentRequest.builder()
                .kycLevel(kycLevel)
                .idType(idType)
                .idNumber(idNumber)
                .idFrontFile(idFrontFile)
                .idBackFile(idBackFile)
                .selfieFile(selfieFile)
                .addressProofFile(addressProofFile)
                .businessLicenseFile(businessLicenseFile)
                .build();

        KycDocument kycDocument = kycService.submitKycDocument(currentUser.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("KYC documents submitted successfully", kycDocument));
    }

    @GetMapping("/me/kyc")
    @Operation(summary = "Get KYC documents", description = "Retrieve user's KYC documents")
    public ResponseEntity<ApiResponse<List<KycDocument>>> getKycDocuments(@AuthenticationPrincipal User currentUser) {
        log.info("Get KYC documents request for user: {}", currentUser.getUserId());

        List<KycDocument> documents = kycService.getUserKycDocuments(currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @PostMapping("/me/2fa/enable")
    @Operation(summary = "Enable 2FA", description = "Enable two-factor authentication for the account")
    public ResponseEntity<ApiResponse<Map<String, String>>> enableTwoFactor(@AuthenticationPrincipal User currentUser) {
        log.info("Enable 2FA request for user: {}", currentUser.getUserId());

        String qrCodeUrl = userService.enableTwoFactorAuth(currentUser.getUserId());

        Map<String, String> response = Map.of(
                "qrCodeUrl", qrCodeUrl,
                "message", "Scan this QR code with your authenticator app");

        return ResponseEntity.ok(ApiResponse.success("2FA enabled successfully", response));
    }

    @PostMapping("/me/2fa/disable")
    @Operation(summary = "Disable 2FA", description = "Disable two-factor authentication for the account")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String twoFactorCode) {

        log.info("Disable 2FA request for user: {}", currentUser.getUserId());

        // TODO: Verify 2FA code before disabling
        userService.disableTwoFactorAuth(currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success("2FA disabled successfully", null));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password", description = "Change the current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {

        log.info("Change password request for user: {}", currentUser.getUserId());

        // TODO: Implement change password in AuthService

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete account", description = "Soft delete the current user's account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@AuthenticationPrincipal User currentUser) {
        log.info("Delete account request for user: {}", currentUser.getUserId());

        userService.deleteUser(currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }

    // Admin endpoints

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Admin: Get any user's profile by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        log.info("Admin: Get user request for ID: {}", userId);

        UserResponse user = userService.getUserById(userId);

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user status", description = "Admin: Update a user's account status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam User.AccountStatus status) {

        log.info("Admin: Update status to {} for user: {}", status, userId);

        userService.updateAccountStatus(userId, status);

        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", null));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check email availability", description = "Check if an email address is available for registration")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmailAvailability(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);

        Map<String, Boolean> response = Map.of("available", available);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/check-phone")
    @Operation(summary = "Check phone availability", description = "Check if a phone number is available for registration")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkPhoneAvailability(@RequestParam String phone) {
        boolean available = userService.isPhoneAvailable(phone);

        Map<String, Boolean> response = Map.of("available", available);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Verifier endpoints

    @GetMapping("/kyc/pending")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    @Operation(summary = "Get pending KYC documents", description = "Verifier: Get all pending KYC documents for review")
    public ResponseEntity<ApiResponse<List<KycDocument>>> getPendingKycDocuments() {
        log.info("Verifier: Get pending KYC documents request");

        List<KycDocument> documents = kycService.getPendingKycDocuments();

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @PostMapping("/kyc/{kycDocId}/approve")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    @Operation(summary = "Approve KYC document", description = "Verifier: Approve a KYC document")
    public ResponseEntity<ApiResponse<Void>> approveKyc(
            @PathVariable UUID kycDocId,
            @AuthenticationPrincipal User reviewer,
            @RequestParam Integer approvedLevel) {

        log.info("Verifier: Approve KYC document {} by {}", kycDocId, reviewer.getUserId());

        kycService.approveKycDocument(kycDocId, reviewer.getUserId(), approvedLevel);

        return ResponseEntity.ok(ApiResponse.success("KYC document approved successfully", null));
    }

    @PostMapping("/kyc/{kycDocId}/reject")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN')")
    @Operation(summary = "Reject KYC document", description = "Verifier: Reject a KYC document with reason")
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            @PathVariable UUID kycDocId,
            @AuthenticationPrincipal User reviewer,
            @RequestParam String reason) {

        log.info("Verifier: Reject KYC document {} by {}", kycDocId, reviewer.getUserId());

        kycService.rejectKycDocument(kycDocId, reviewer.getUserId(), reason);

        return ResponseEntity.ok(ApiResponse.success("KYC document rejected", null));
    }
}
