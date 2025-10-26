package com.carbonmarketplace.userservice.controller;

import com.carbonmarketplace.userservice.dto.request.LoginRequest;
import com.carbonmarketplace.userservice.dto.request.RefreshTokenRequest;
import com.carbonmarketplace.userservice.dto.request.RegisterRequest;
import com.carbonmarketplace.userservice.dto.response.AuthResponse;
import com.carbonmarketplace.userservice.dto.response.UserResponse;
import com.carbonmarketplace.userservice.exception.EmailAlreadyExistsException;
import com.carbonmarketplace.userservice.security.JwtAuthenticationFilter;
import com.carbonmarketplace.userservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for testing
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private AuthResponse authResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        // Setup RegisterRequest
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFullName("Test User");
        registerRequest.setRole("EVOWNER");
        registerRequest.setPhone("+1234567890");

        // Setup LoginRequest
        loginRequest = new LoginRequest();
        loginRequest.setUsername("test@example.com");
        loginRequest.setPassword("Password123!");

        // Setup RefreshTokenRequest
        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh.token.here");

        // Setup UserResponse
        userResponse = new UserResponse();
        userResponse.setUserId(UUID.randomUUID());
        userResponse.setEmail("test@example.com");
        userResponse.setFullName("Test User");
        userResponse.setRole("DRIVER");

        // Setup AuthResponse
        authResponse = AuthResponse.success(
                "access.token.here",
                "refresh.token.here",
                3600000L,
                userResponse);
    }

    // ===== POST /auth/register Tests =====

    @Test
    @DisplayName("POST /auth/register - Should register user successfully")
    void register_ShouldRegisterSuccessfully_WithValidData() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user").exists())
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Should return 409 when email already exists")
    void register_ShouldReturnConflict_WhenEmailExists() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email is already registered"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 with invalid request")
    void register_ShouldReturnBadRequest_WithInvalidData() throws Exception {
        // Arrange - Missing required fields
        registerRequest.setEmail(null);
        registerRequest.setPassword(null);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Should handle registration without phone")
    void register_ShouldRegisterSuccessfully_WithoutPhone() throws Exception {
        // Arrange
        registerRequest.setPhone(null);
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    // ===== POST /auth/login Tests =====

    @Test
    @DisplayName("POST /auth/login - Should login successfully with valid credentials")
    void login_ShouldLoginSuccessfully_WithValidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user").exists());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should require 2FA when enabled")
    void login_ShouldRequireTwoFactor_When2FAEnabled() throws Exception {
        // Arrange
        AuthResponse twoFactorRequired = AuthResponse.requiresTwoFactor();
        when(authService.login(any(LoginRequest.class))).thenReturn(twoFactorRequired);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Two-factor authentication required"))
                .andExpect(jsonPath("$.data.requiresTwoFactor").value(true));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 401 with invalid credentials")
    void login_ShouldReturnUnauthorized_WithInvalidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 400 with missing fields")
    void login_ShouldReturnBadRequest_WithMissingFields() throws Exception {
        // Arrange
        loginRequest.setUsername(null);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should handle login with 2FA code")
    void login_ShouldLoginSuccessfully_With2FACode() throws Exception {
        // Arrange
        loginRequest.setTwoFactorCode("123456");
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // ===== POST /auth/refresh Tests =====

    @Test
    @DisplayName("POST /auth/refresh - Should refresh token successfully")
    void refreshToken_ShouldRefreshSuccessfully_WithValidToken() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("POST /auth/refresh - Should return 401 with invalid token")
    void refreshToken_ShouldReturnUnauthorized_WithInvalidToken() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("POST /auth/refresh - Should return 400 with missing token")
    void refreshToken_ShouldReturnBadRequest_WithMissingToken() throws Exception {
        // Arrange
        refreshTokenRequest.setRefreshToken(null);

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).refreshToken(any(RefreshTokenRequest.class));
    }

    // ===== POST /auth/logout Tests =====

    @Test
    @DisplayName("POST /auth/logout - Should logout successfully with refresh token")
    void logout_ShouldLogoutSuccessfully_WithRefreshToken() throws Exception {
        // Arrange
        String refreshToken = "refresh.token.here";
        doNothing().when(authService).logout(refreshToken);

        // Act & Assert
        mockMvc.perform(post("/auth/logout")
                .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService, times(1)).logout(refreshToken);
    }

    @Test
    @DisplayName("POST /auth/logout - Should logout successfully without refresh token")
    void logout_ShouldLogoutSuccessfully_WithoutRefreshToken() throws Exception {
        // Arrange
        doNothing().when(authService).logout(null);

        // Act & Assert
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService, times(1)).logout(null);
    }

    // ===== POST /auth/forgot-password Tests =====

    @Test
    @DisplayName("POST /auth/forgot-password - Should send reset email successfully")
    void forgotPassword_ShouldSendEmailSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/forgot-password")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset email sent"));
    }

    @Test
    @DisplayName("POST /auth/forgot-password - Should return 400 without email")
    void forgotPassword_ShouldReturnBadRequest_WithoutEmail() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/forgot-password"))
                .andExpect(status().isBadRequest());
    }

    // ===== POST /auth/reset-password Tests =====

    @Test
    @DisplayName("POST /auth/reset-password - Should reset password successfully")
    void resetPassword_ShouldResetSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/reset-password")
                .param("token", "reset.token.here")
                .param("newPassword", "NewPassword123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    @DisplayName("POST /auth/reset-password - Should return 400 without token")
    void resetPassword_ShouldReturnBadRequest_WithoutToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/reset-password")
                .param("newPassword", "NewPassword123!"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/reset-password - Should return 400 without password")
    void resetPassword_ShouldReturnBadRequest_WithoutPassword() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/reset-password")
                .param("token", "reset.token.here"))
                .andExpect(status().isBadRequest());
    }

    // ===== GET /auth/verify-email/{token} Tests =====

    @Test
    @DisplayName("GET /auth/verify-email/{token} - Should verify email successfully")
    void verifyEmail_ShouldVerifySuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/auth/verify-email/{token}", "verification.token.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    // ===== POST /auth/resend-verification Tests =====

    @Test
    @DisplayName("POST /auth/resend-verification - Should resend verification email")
    void resendVerification_ShouldResendSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/resend-verification")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification email sent"));
    }

    @Test
    @DisplayName("POST /auth/resend-verification - Should return 400 without email")
    void resendVerification_ShouldReturnBadRequest_WithoutEmail() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/resend-verification"))
                .andExpect(status().isBadRequest());
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("POST /auth/register - Should handle malformed JSON")
    void register_ShouldReturnBadRequest_WithMalformedJson() throws Exception {
        // Arrange
        String malformedJson = "{invalid json}";

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should handle malformed JSON")
    void login_ShouldReturnBadRequest_WithMalformedJson() throws Exception {
        // Arrange
        String malformedJson = "{invalid json}";

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Should handle buyer registration with company info")
    void register_ShouldRegisterBuyer_WithCompanyInfo() throws Exception {
        // Arrange
        registerRequest.setRole("BUYER");
        registerRequest.setCompanyName("Test Company");
        registerRequest.setTaxCode("TAX123");
        registerRequest.setBusinessRegistrationNumber("REG456");

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should handle login with device info and IP")
    void login_ShouldHandleDeviceInfo() throws Exception {
        // Arrange
        loginRequest.setDeviceInfo("Chrome on Windows");
        loginRequest.setIpAddress("192.168.1.1");
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }
}
