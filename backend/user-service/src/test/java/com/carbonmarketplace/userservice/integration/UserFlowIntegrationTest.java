package com.carbonmarketplace.userservice.integration;

import com.carbonmarketplace.userservice.dto.request.LoginRequest;
import com.carbonmarketplace.userservice.dto.request.RefreshTokenRequest;
import com.carbonmarketplace.userservice.dto.request.RegisterRequest;
import com.carbonmarketplace.userservice.dto.request.UpdateUserRequest;
import com.carbonmarketplace.userservice.dto.response.AuthResponse;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.repository.RefreshTokenRepository;
import com.carbonmarketplace.userservice.repository.UserRepository;
import com.carbonmarketplace.userservice.service.AuthService;
import com.carbonmarketplace.userservice.service.EmailService;
import com.carbonmarketplace.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for complete user flows:
 * - Registration flow
 * - Login flow with OTP verification
 * - KYC submission flow
 * - Complete user journey from registration to verified user
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("User Flow Integration Tests")
class UserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @MockBean
    private EmailService emailService;

    private static String testEmail;
    private static String testPassword;
    private static String accessToken;
    private static String refreshToken;
    private static UUID userId;

    @BeforeAll
    static void setupTestData() {
        testEmail = "integration.test." + System.currentTimeMillis() + "@example.com";
        testPassword = "TestPassword123!";
    }

    @BeforeEach
    void setUp() {
        // Mock email service to avoid actual email sending
        doNothing().when(emailService).sendVerificationEmail(any(User.class));
    }

    // ===== Registration Flow Tests =====

    @Test
    @Order(1)
    @DisplayName("Complete Registration Flow - Should register new user successfully")
    @Transactional
    void testRegistrationFlow() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);
        registerRequest.setFullName("Integration Test User");
        registerRequest.setRole("EVOWNER");
        registerRequest.setPhone("+1234567890");

        // Act & Assert - Register user
        String response = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user").exists())
                .andExpect(jsonPath("$.data.user.email").value(testEmail))
                .andExpect(jsonPath("$.data.user.fullName").value("Integration Test User"))
                .andExpect(jsonPath("$.data.user.role").value("EVOWNER"))
                .andExpect(jsonPath("$.data.user.emailVerified").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract tokens and userId from response
        AuthResponse authResponse = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                AuthResponse.class);

        accessToken = authResponse.getAccessToken();
        refreshToken = authResponse.getRefreshToken();
        userId = authResponse.getUser().getUserId();

        // Verify user was created in database
        User createdUser = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(createdUser, "User should be created in database");
        assertEquals(testEmail, createdUser.getEmail());
        assertEquals("Integration Test User", createdUser.getFullName());
        assertEquals(User.UserRole.EVOWNER, createdUser.getRole());
        assertEquals(User.KycStatus.PENDING, createdUser.getKycStatus());
        assertEquals(0, createdUser.getKycLevel());
        assertFalse(createdUser.getEmailVerified());

        // Verify refresh token was created
        assertTrue(refreshTokenRepository.findByToken(refreshToken).isPresent());
    }

    @Test
    @Order(2)
    @DisplayName("Registration Flow - Should prevent duplicate email registration")
    @Transactional
    void testDuplicateEmailRegistration() throws Exception {
        // Arrange - Try to register with same email
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setEmail(testEmail);
        duplicateRequest.setPassword("AnotherPassword123!");
        duplicateRequest.setFullName("Duplicate User");
        duplicateRequest.setRole("EVOWNER");

        // Act & Assert - Should fail with conflict
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    // ===== Login Flow Tests =====

    @Test
    @Order(3)
    @DisplayName("Complete Login Flow - Should login successfully with valid credentials")
    @Transactional
    void testLoginFlow() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testEmail);
        loginRequest.setPassword(testPassword);
        loginRequest.setDeviceInfo("Chrome on Windows");
        loginRequest.setIpAddress("192.168.1.1");

        // Act & Assert - Login
        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value(testEmail))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Update tokens for subsequent tests
        AuthResponse authResponse = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                AuthResponse.class);
        accessToken = authResponse.getAccessToken();
        refreshToken = authResponse.getRefreshToken();

        // Verify last login timestamp was updated
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(user);
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    @Order(4)
    @DisplayName("Login Flow - Should fail with invalid password")
    @Transactional
    void testLoginWithInvalidPassword() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testEmail);
        loginRequest.setPassword("WrongPassword123!");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        // Verify failed login attempts were incremented
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(user);
        assertTrue(user.getFailedLoginAttempts() > 0);
    }

    @Test
    @Order(5)
    @DisplayName("Login Flow - Should lock account after max failed attempts")
    @Transactional
    void testAccountLockAfterMaxFailedAttempts() throws Exception {
        // Arrange - Reset failed attempts first
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        user.setFailedLoginAttempts(4); // Set to 4, next attempt will lock
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testEmail);
        loginRequest.setPassword("WrongPassword123!");

        // Act & Assert - Should lock account
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isLocked());

        // Verify account is locked
        user = userRepository.findByEmail(testEmail).orElseThrow();
        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());

        // Reset for subsequent tests
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    // ===== Token Refresh Flow Tests =====

    @Test
    @Order(6)
    @DisplayName("Token Refresh Flow - Should refresh access token successfully")
    @Transactional
    void testTokenRefreshFlow() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);
        refreshRequest.setDeviceInfo("Chrome on Windows");

        // Act & Assert
        String response = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Update tokens
        AuthResponse authResponse = objectMapper.readValue(
                objectMapper.readTree(response).get("data").toString(),
                AuthResponse.class);

        // Verify old refresh token was revoked
        assertTrue(refreshTokenRepository.findByToken(refreshToken)
                .map(token -> token.getRevoked())
                .orElse(true));

        // Update to new tokens
        accessToken = authResponse.getAccessToken();
        refreshToken = authResponse.getRefreshToken();
    }

    @Test
    @Order(7)
    @DisplayName("Token Refresh Flow - Should fail with invalid refresh token")
    @Transactional
    void testTokenRefreshWithInvalidToken() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid.refresh.token");

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ===== User Profile Management Tests =====

    @Test
    @Order(8)
    @DisplayName("Profile Management - Should update user profile successfully")
    @Transactional
    void testUpdateProfileFlow() throws Exception {
        // Arrange
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("Updated Test User");
        updateRequest.setBio("This is my updated bio");

        // Act
        userService.updateUser(userId, updateRequest);

        // Assert - Verify updates in database
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals("Updated Test User", updatedUser.getFullName());
        assertEquals("This is my updated bio", updatedUser.getBio());
    }

    @Test
    @Order(9)
    @DisplayName("Profile Management - Should verify email successfully")
    @Transactional
    void testEmailVerificationFlow() throws Exception {
        // Act
        userService.verifyEmail(userId);

        // Assert - Verify email was verified
        User user = userRepository.findById(userId).orElseThrow();
        assertTrue(user.getEmailVerified());
    }

    @Test
    @Order(10)
    @DisplayName("Profile Management - Should verify phone successfully")
    @Transactional
    void testPhoneVerificationFlow() throws Exception {
        // Act
        userService.verifyPhone(userId);

        // Assert - Verify phone was verified
        User user = userRepository.findById(userId).orElseThrow();
        assertTrue(user.getPhoneVerified());
    }

    // ===== Two-Factor Authentication Flow Tests =====

    @Test
    @Order(11)
    @DisplayName("2FA Flow - Should enable two-factor authentication")
    @Transactional
    void testEnable2FAFlow() throws Exception {
        // Act
        String qrCodeUrl = userService.enableTwoFactorAuth(userId);

        // Assert
        assertNotNull(qrCodeUrl);
        assertTrue(qrCodeUrl.contains("otpauth://totp/"));
        assertTrue(qrCodeUrl.contains(testEmail));

        // Verify in database
        User user = userRepository.findById(userId).orElseThrow();
        assertTrue(user.getTwoFactorEnabled());
        assertNotNull(user.getTwoFactorSecret());
    }

    @Test
    @Order(12)
    @DisplayName("2FA Flow - Should disable two-factor authentication")
    @Transactional
    void testDisable2FAFlow() throws Exception {
        // Act
        userService.disableTwoFactorAuth(userId);

        // Assert - Verify in database
        User user = userRepository.findById(userId).orElseThrow();
        assertFalse(user.getTwoFactorEnabled());
        assertNull(user.getTwoFactorSecret());
    }

    // ===== Password Management Tests =====

    @Test
    @Order(13)
    @DisplayName("Password Management - Should change password successfully")
    @Transactional
    void testChangePasswordFlow() throws Exception {
        // Arrange
        String newPassword = "NewPassword456!";

        // Act
        authService.changePassword(userId, testPassword, newPassword);

        // Assert - Try to login with new password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testEmail);
        loginRequest.setPassword(newPassword);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Update password for subsequent tests
        testPassword = newPassword;
    }

    @Test
    @Order(14)
    @DisplayName("Password Management - Should fail to change with wrong current password")
    @Transactional
    void testChangePasswordWithWrongCurrentPassword() throws Exception {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            authService.changePassword(userId, "WrongPassword!", "NewPassword789!");
        });
    }

    // ===== Account Management Tests =====

    @Test
    @Order(15)
    @DisplayName("Account Management - Should check email availability")
    @Transactional
    void testCheckEmailAvailability() throws Exception {
        // Act & Assert - Existing email
        boolean existingEmailAvailable = userService.isEmailAvailable(testEmail);
        assertFalse(existingEmailAvailable);

        // Act & Assert - New email
        boolean newEmailAvailable = userService.isEmailAvailable("newemail@example.com");
        assertTrue(newEmailAvailable);
    }

    @Test
    @Order(16)
    @DisplayName("Account Management - Should check phone availability")
    @Transactional
    void testCheckPhoneAvailability() throws Exception {
        // Act & Assert - Existing phone
        boolean existingPhoneAvailable = userService.isPhoneAvailable("+1234567890");
        assertFalse(existingPhoneAvailable);

        // Act & Assert - New phone
        boolean newPhoneAvailable = userService.isPhoneAvailable("+9999999999");
        assertTrue(newPhoneAvailable);
    }

    // ===== Logout Flow Tests =====

    @Test
    @Order(17)
    @DisplayName("Logout Flow - Should logout successfully")
    @Transactional
    void testLogoutFlow() throws Exception {
        // Act
        authService.logout(refreshToken);

        // Assert - Verify refresh token was revoked
        assertTrue(refreshTokenRepository.findByToken(refreshToken)
                .map(token -> token.getRevoked())
                .orElse(true));
    }

    @Test
    @Order(18)
    @DisplayName("Logout Flow - Should logout all sessions")
    @Transactional
    void testLogoutAllSessions() throws Exception {
        // Arrange - Login again to create a new session
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testEmail);
        loginRequest.setPassword(testPassword);

        authService.login(loginRequest);

        // Act - Logout all sessions
        authService.logoutAll(userId);

        // Assert - Verify all refresh tokens were revoked
        long activeTokens = refreshTokenRepository.findByToken(refreshToken)
                .stream()
                .filter(token -> !token.getRevoked())
                .count();

        assertEquals(0, activeTokens);
    }

    // ===== Soft Delete Tests =====

    @Test
    @Order(19)
    @DisplayName("Account Management - Should soft delete user account")
    @Transactional
    void testSoftDeleteAccount() throws Exception {
        // Act
        userService.deleteUser(userId);

        // Assert - Verify user was soft deleted
        User user = userRepository.findById(userId).orElseThrow();
        assertNotNull(user.getDeletedAt());
        assertEquals(User.AccountStatus.CLOSED, user.getAccountStatus());

        // Verify user cannot be found by active query
        assertTrue(userRepository.findActiveById(userId).isEmpty());
    }

    // ===== Complete User Journey Test =====

    @Test
    @Order(20)
    @DisplayName("Complete User Journey - From registration to verified user")
    @Transactional
    void testCompleteUserJourney() throws Exception {
        // Create a unique test email for this journey
        String journeyEmail = "journey." + System.currentTimeMillis() + "@example.com";
        String journeyPassword = "JourneyPassword123!";

        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(journeyEmail);
        registerRequest.setPassword(journeyPassword);
        registerRequest.setFullName("Journey Test User");
        registerRequest.setRole("EVOWNER");
        registerRequest.setPhone("+9876543210");

        AuthResponse registerResponse = authService.register(registerRequest);
        assertNotNull(registerResponse);
        UUID journeyUserId = registerResponse.getUser().getUserId();

        // Step 2: Verify Email
        userService.verifyEmail(journeyUserId);
        User user = userRepository.findById(journeyUserId).orElseThrow();
        assertTrue(user.getEmailVerified());

        // Step 3: Verify Phone
        userService.verifyPhone(journeyUserId);
        user = userRepository.findById(journeyUserId).orElseThrow();
        assertTrue(user.getPhoneVerified());

        // Step 4: Enable 2FA
        String qrCode = userService.enableTwoFactorAuth(journeyUserId);
        assertNotNull(qrCode);
        user = userRepository.findById(journeyUserId).orElseThrow();
        assertTrue(user.getTwoFactorEnabled());

        // Step 5: Update Profile
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("Journey Updated User");
        updateRequest.setBio("Completed my journey!");
        userService.updateUser(journeyUserId, updateRequest);

        user = userRepository.findById(journeyUserId).orElseThrow();
        assertEquals("Journey Updated User", user.getFullName());
        assertEquals("Completed my journey!", user.getBio());

        // Step 6: Login (without 2FA code for simplicity in this test)
        // In real scenario, 2FA code would be required
        user.setTwoFactorEnabled(false); // Temporarily disable for login test
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(journeyEmail);
        loginRequest.setPassword(journeyPassword);

        AuthResponse loginResponse = authService.login(loginRequest);
        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getAccessToken());

        // Verify complete journey
        user = userRepository.findById(journeyUserId).orElseThrow();
        assertTrue(user.getEmailVerified());
        assertTrue(user.getPhoneVerified());
        assertEquals("Journey Updated User", user.getFullName());
        assertEquals(User.AccountStatus.ACTIVE, user.getAccountStatus());
        assertNotNull(user.getLastLoginAt());
    }

    @AfterAll
    static void cleanup(@Autowired UserRepository userRepository) {
        // Cleanup test data
        try {
            userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
