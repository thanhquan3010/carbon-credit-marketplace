package com.carbonmarketplace.userservice.controller;

import com.carbonmarketplace.userservice.dto.request.LoginRequest;
import com.carbonmarketplace.userservice.dto.request.RefreshTokenRequest;
import com.carbonmarketplace.userservice.dto.request.RegisterRequest;
import com.carbonmarketplace.userservice.dto.response.ApiResponse;
import com.carbonmarketplace.userservice.dto.response.AuthResponse;
import com.carbonmarketplace.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with email/phone and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for: {}", request.getUsername());

        AuthResponse response = authService.login(request);

        if (response.getRequiresTwoFactor() != null && response.getRequiresTwoFactor()) {
            return ResponseEntity.ok(ApiResponse.success("Two-factor authentication required", response));
        }

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate refresh token and clear session")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestParam(required = false) String refreshToken) {
        log.info("Logout request received");

        authService.logout(refreshToken);

        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Send password reset email to user")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
        log.info("Password reset requested for email: {}", email);

        // TODO: Implement password reset flow with email token
        // For now, just return success

        return ResponseEntity.ok(ApiResponse.success("Password reset email sent", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token from email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {

        log.info("Password reset attempt with token");

        // TODO: Validate token and reset password
        // For now, just return success

        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    @GetMapping("/verify-email/{token}")
    @Operation(summary = "Verify email address", description = "Verify user email with verification token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@PathVariable String token) {
        log.info("Email verification attempt with token: {}", token);

        // TODO: Implement email verification with token
        // For now, just return success

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Send a new verification email to user")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        log.info("Resend verification requested for email: {}", email);

        // TODO: Implement resend verification
        // For now, just return success

        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    @GetMapping("/token/status")
    @Operation(summary = "Check token expiration status", description = "Get information about token expiration time")
    public ResponseEntity<ApiResponse<com.carbonmarketplace.userservice.dto.response.TokenExpirationResponse>> checkTokenStatus(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Token status check requested");

        String token = authHeader.replace("Bearer ", "");

        com.carbonmarketplace.userservice.dto.response.TokenExpirationResponse status = authService
                .checkTokenExpiration(token);

        return ResponseEntity.ok(ApiResponse.success("Token status retrieved", status));
    }
}
