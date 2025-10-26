package com.carbonmarketplace.userservice.controller;

import com.carbonmarketplace.userservice.dto.request.UpdateUserRequest;
import com.carbonmarketplace.userservice.dto.response.UserResponse;
import com.carbonmarketplace.userservice.entity.KycDocument;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.exception.UserNotFoundException;
import com.carbonmarketplace.userservice.security.JwtAuthenticationFilter;
import com.carbonmarketplace.userservice.service.KycService;
import com.carbonmarketplace.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for testing
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private KycService kycService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private UserResponse userResponse;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testUser = User.builder()
                .userId(testUserId)
                .email("test@example.com")
                .phone("+1234567890")
                .fullName("Test User")
                .role(User.UserRole.EVOWNER)
                .kycLevel(0)
                .kycStatus(User.KycStatus.PENDING)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .build();

        userResponse = UserResponse.fromEntity(testUser);
    }

    // ===== GET /users/me Tests =====

    @Test
    @DisplayName("GET /users/me - Should return current user profile")
    @WithMockUser
    void getCurrentUser_ShouldReturnCurrentUser() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/users/me")
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()));
    }

    // ===== PATCH /users/me Tests =====

    @Test
    @DisplayName("PATCH /users/me - Should update user profile successfully")
    @WithMockUser
    void updateProfile_ShouldUpdateSuccessfully() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setBio("New bio");

        when(userService.updateUser(eq(testUserId), any(UpdateUserRequest.class))).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(patch("/users/me")
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(userService, times(1)).updateUser(eq(testUserId), any(UpdateUserRequest.class));
    }

    // ===== POST /users/me/kyc Tests =====

    @Test
    @DisplayName("POST /users/me/kyc - Should submit KYC successfully")
    @WithMockUser
    void submitKyc_ShouldSubmitSuccessfully() throws Exception {
        // Arrange
        KycDocument kycDocument = KycDocument.builder()
                .kycDocId(UUID.randomUUID())
                .user(testUser)
                .kycLevel(1)
                .idType(KycDocument.IdType.PASSPORT)
                .idNumber("AB123456")
                .status(KycDocument.VerificationStatus.PENDING)
                .build();

        when(kycService.submitKycDocument(eq(testUserId), any())).thenReturn(kycDocument);

        // Act & Assert
        mockMvc.perform(multipart("/users/me/kyc")
                .with(user(testUser))
                .param("kycLevel", "1")
                .param("idType", "PASSPORT")
                .param("idNumber", "AB123456")
                .content("test".getBytes()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("KYC documents submitted successfully"));

        verify(kycService, times(1)).submitKycDocument(eq(testUserId), any());
    }

    // ===== GET /users/me/kyc Tests =====

    @Test
    @DisplayName("GET /users/me/kyc - Should return user's KYC documents")
    @WithMockUser
    void getKycDocuments_ShouldReturnDocuments() throws Exception {
        // Arrange
        List<KycDocument> documents = Arrays.asList(
                KycDocument.builder()
                        .kycDocId(UUID.randomUUID())
                        .kycLevel(1)
                        .status(KycDocument.VerificationStatus.APPROVED)
                        .build());

        when(kycService.getUserKycDocuments(testUserId)).thenReturn(documents);

        // Act & Assert
        mockMvc.perform(get("/users/me/kyc")
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].kycLevel").value(1));

        verify(kycService, times(1)).getUserKycDocuments(testUserId);
    }

    // ===== POST /users/me/2fa/enable Tests =====

    @Test
    @DisplayName("POST /users/me/2fa/enable - Should enable 2FA successfully")
    @WithMockUser
    void enableTwoFactor_ShouldEnableSuccessfully() throws Exception {
        // Arrange
        String qrCodeUrl = "otpauth://totp/test@example.com?secret=SECRET123&issuer=CarbonMarketplace";
        when(userService.enableTwoFactorAuth(testUserId)).thenReturn(qrCodeUrl);

        // Act & Assert
        mockMvc.perform(post("/users/me/2fa/enable")
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("2FA enabled successfully"))
                .andExpect(jsonPath("$.data.qrCodeUrl").exists());

        verify(userService, times(1)).enableTwoFactorAuth(testUserId);
    }

    // ===== POST /users/me/2fa/disable Tests =====

    @Test
    @DisplayName("POST /users/me/2fa/disable - Should disable 2FA successfully")
    @WithMockUser
    void disableTwoFactor_ShouldDisableSuccessfully() throws Exception {
        // Arrange
        doNothing().when(userService).disableTwoFactorAuth(testUserId);

        // Act & Assert
        mockMvc.perform(post("/users/me/2fa/disable")
                .with(user(testUser))
                .param("twoFactorCode", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("2FA disabled successfully"));

        verify(userService, times(1)).disableTwoFactorAuth(testUserId);
    }

    // ===== DELETE /users/me Tests =====

    @Test
    @DisplayName("DELETE /users/me - Should delete account successfully")
    @WithMockUser
    void deleteAccount_ShouldDeleteSuccessfully() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/users/me")
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));

        verify(userService, times(1)).deleteUser(testUserId);
    }

    // ===== Admin Endpoints Tests =====

    @Test
    @DisplayName("GET /users/{userId} - Should return user by ID (Admin)")
    @WithMockUser(roles = "ADMIN")
    void getUserById_ShouldReturnUser_AsAdmin() throws Exception {
        // Arrange
        when(userService.getUserById(testUserId)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/users/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").exists());

        verify(userService, times(1)).getUserById(testUserId);
    }

    @Test
    @DisplayName("GET /users/{userId} - Should throw exception when user not found")
    @WithMockUser(roles = "ADMIN")
    void getUserById_ShouldThrowException_WhenUserNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(testUserId)).thenThrow(new UserNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/users/{userId}", testUserId))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById(testUserId);
    }

    @Test
    @DisplayName("PATCH /users/{userId}/status - Should update user status (Admin)")
    @WithMockUser(roles = "ADMIN")
    void updateUserStatus_ShouldUpdateSuccessfully_AsAdmin() throws Exception {
        // Arrange
        doNothing().when(userService).updateAccountStatus(testUserId, User.AccountStatus.SUSPENDED);

        // Act & Assert
        mockMvc.perform(patch("/users/{userId}/status", testUserId)
                .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User status updated successfully"));

        verify(userService, times(1)).updateAccountStatus(testUserId, User.AccountStatus.SUSPENDED);
    }

    // ===== Email/Phone Availability Tests =====

    @Test
    @DisplayName("GET /users/check-email - Should return availability status")
    void checkEmailAvailability_ShouldReturnStatus() throws Exception {
        // Arrange
        when(userService.isEmailAvailable("test@example.com")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/users/check-email")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));

        verify(userService, times(1)).isEmailAvailable("test@example.com");
    }

    @Test
    @DisplayName("GET /users/check-phone - Should return availability status")
    void checkPhoneAvailability_ShouldReturnStatus() throws Exception {
        // Arrange
        when(userService.isPhoneAvailable("+1234567890")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/users/check-phone")
                .param("phone", "+1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));

        verify(userService, times(1)).isPhoneAvailable("+1234567890");
    }

    // ===== Verifier Endpoints Tests =====

    @Test
    @DisplayName("GET /users/kyc/pending - Should return pending KYC documents (Verifier)")
    @WithMockUser(roles = "VERIFIER")
    void getPendingKycDocuments_ShouldReturnDocuments_AsVerifier() throws Exception {
        // Arrange
        List<KycDocument> documents = Arrays.asList(
                KycDocument.builder()
                        .kycDocId(UUID.randomUUID())
                        .status(KycDocument.VerificationStatus.PENDING)
                        .build());

        when(kycService.getPendingKycDocuments()).thenReturn(documents);

        // Act & Assert
        mockMvc.perform(get("/users/kyc/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(kycService, times(1)).getPendingKycDocuments();
    }

    @Test
    @DisplayName("POST /users/kyc/{kycDocId}/approve - Should approve KYC (Verifier)")
    @WithMockUser(roles = "VERIFIER")
    void approveKyc_ShouldApproveSuccessfully_AsVerifier() throws Exception {
        // Arrange
        UUID kycDocId = UUID.randomUUID();
        User reviewer = User.builder().userId(UUID.randomUUID()).role(User.UserRole.VERIFIER).build();

        doNothing().when(kycService).approveKycDocument(eq(kycDocId), any(UUID.class), eq(1));

        // Act & Assert
        mockMvc.perform(post("/users/kyc/{kycDocId}/approve", kycDocId)
                .with(user(reviewer))
                .param("approvedLevel", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("KYC document approved successfully"));

        verify(kycService, times(1)).approveKycDocument(eq(kycDocId), any(UUID.class), eq(1));
    }

    @Test
    @DisplayName("POST /users/kyc/{kycDocId}/reject - Should reject KYC (Verifier)")
    @WithMockUser(roles = "VERIFIER")
    void rejectKyc_ShouldRejectSuccessfully_AsVerifier() throws Exception {
        // Arrange
        UUID kycDocId = UUID.randomUUID();
        User reviewer = User.builder().userId(UUID.randomUUID()).role(User.UserRole.VERIFIER).build();
        String rejectionReason = "Document is blurry";

        doNothing().when(kycService).rejectKycDocument(eq(kycDocId), any(UUID.class), eq(rejectionReason));

        // Act & Assert
        mockMvc.perform(post("/users/kyc/{kycDocId}/reject", kycDocId)
                .with(user(reviewer))
                .param("reason", rejectionReason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("KYC document rejected"));

        verify(kycService, times(1)).rejectKycDocument(eq(kycDocId), any(UUID.class), eq(rejectionReason));
    }

    // ===== Negative Test Cases =====

    @Test
    @DisplayName("PATCH /users/me - Should return 400 with invalid request")
    @WithMockUser
    void updateProfile_ShouldReturnBadRequest_WithInvalidData() throws Exception {
        // Arrange - Invalid JSON
        String invalidJson = "{invalid json}";

        // Act & Assert
        mockMvc.perform(patch("/users/me")
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(any(UUID.class), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("DELETE /users/me - Should handle service exception")
    @WithMockUser
    void deleteAccount_ShouldHandleException() throws Exception {
        // Arrange
        doThrow(new UserNotFoundException("User not found")).when(userService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/users/me")
                .with(user(testUser)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteUser(testUserId);
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("GET /users/me/kyc - Should return empty list when no KYC documents")
    @WithMockUser
    void getKycDocuments_ShouldReturnEmptyList_WhenNoDocuments() throws Exception {
        // Arrange
        when(kycService.getUserKycDocuments(testUserId)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/users/me/kyc")
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(kycService, times(1)).getUserKycDocuments(testUserId);
    }

    @Test
    @DisplayName("GET /users/kyc/pending - Should return empty list when no pending documents")
    @WithMockUser(roles = "VERIFIER")
    void getPendingKycDocuments_ShouldReturnEmptyList_WhenNoPending() throws Exception {
        // Arrange
        when(kycService.getPendingKycDocuments()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/users/kyc/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(kycService, times(1)).getPendingKycDocuments();
    }
}
