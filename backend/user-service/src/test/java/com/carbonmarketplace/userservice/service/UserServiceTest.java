package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.dto.request.UpdateUserRequest;
import com.carbonmarketplace.userservice.dto.response.UserResponse;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.exception.UserNotFoundException;
import com.carbonmarketplace.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;
    private String testEmail;
    private String testPhone;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testPhone = "+1234567890";

        testUser = User.builder()
                .userId(testUserId)
                .email(testEmail)
                .phone(testPhone)
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .role(User.UserRole.EVOWNER)
                .kycLevel(0)
                .kycStatus(User.KycStatus.PENDING)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .build();
    }

    // ===== Positive Test Cases =====

    @Test
    @DisplayName("getUserById - Should return user when user exists")
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Arrange
        when(userRepository.findActiveById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = userService.getUserById(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testEmail, result.getEmail());
        verify(userRepository, times(1)).findActiveById(testUserId);
    }

    @Test
    @DisplayName("getUserByEmail - Should return user when user exists")
    void getUserByEmail_ShouldReturnUser_WhenUserExists() {
        // Arrange
        when(userRepository.findActiveByEmail(testEmail.toLowerCase())).thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = userService.getUserByEmail(testEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        verify(userRepository, times(1)).findActiveByEmail(testEmail.toLowerCase());
    }

    @Test
    @DisplayName("updateUser - Should update user successfully with valid data")
    void updateUser_ShouldUpdateSuccessfully_WithValidData() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setBio("New bio");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.updateUser(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Should update phone and reset verification when phone changes")
    void updateUser_ShouldResetPhoneVerification_WhenPhoneChanges() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("+9876543210");

        testUser.setPhoneVerified(true); // User has verified phone initially

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertFalse(user.getPhoneVerified(), "Phone verification should be reset");
            return user;
        });

        // Act
        UserResponse result = userService.updateUser(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).existsByPhone(request.getPhone());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Should update buyer-specific fields for buyer role")
    void updateUser_ShouldUpdateBuyerFields_ForBuyerRole() {
        // Arrange
        testUser.setRole(User.UserRole.BUYER);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setCompanyName("Test Company");
        request.setTaxCode("TAX123");
        request.setBusinessRegistrationNumber("REG456");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.updateUser(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("deleteUser - Should soft delete user successfully")
    void deleteUser_ShouldSoftDelete_Successfully() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertNotNull(user.getDeletedAt(), "Deleted timestamp should be set");
            assertEquals(User.AccountStatus.CLOSED, user.getAccountStatus(), "Account should be closed");
            return user;
        });

        // Act
        userService.deleteUser(testUserId);

        // Assert
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("getAllUsers - Should return paginated users")
    void getAllUsers_ShouldReturnPaginatedUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        User user2 = User.builder()
                .userId(UUID.randomUUID())
                .email("test2@example.com")
                .fullName("Test User 2")
                .build();
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser, user2));

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // Act
        Page<UserResponse> result = userService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("verifyEmail - Should verify user email successfully")
    void verifyEmail_ShouldVerifySuccessfully() {
        // Arrange
        doNothing().when(userRepository).verifyEmail(testUserId);

        // Act
        userService.verifyEmail(testUserId);

        // Assert
        verify(userRepository, times(1)).verifyEmail(testUserId);
    }

    @Test
    @DisplayName("verifyPhone - Should verify user phone successfully")
    void verifyPhone_ShouldVerifySuccessfully() {
        // Arrange
        doNothing().when(userRepository).verifyPhone(testUserId);

        // Act
        userService.verifyPhone(testUserId);

        // Assert
        verify(userRepository, times(1)).verifyPhone(testUserId);
    }

    @Test
    @DisplayName("enableTwoFactorAuth - Should enable 2FA and return QR code URL")
    void enableTwoFactorAuth_ShouldReturnQRCodeUrl() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertTrue(user.getTwoFactorEnabled(), "2FA should be enabled");
            assertNotNull(user.getTwoFactorSecret(), "2FA secret should be set");
            return user;
        });

        // Act
        String result = userService.enableTwoFactorAuth(testUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("otpauth://totp/"));
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("disableTwoFactorAuth - Should disable 2FA successfully")
    void disableTwoFactorAuth_ShouldDisableSuccessfully() {
        // Arrange
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("SECRET123");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertFalse(user.getTwoFactorEnabled(), "2FA should be disabled");
            assertNull(user.getTwoFactorSecret(), "2FA secret should be cleared");
            return user;
        });

        // Act
        userService.disableTwoFactorAuth(testUserId);

        // Assert
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateAccountStatus - Should update account status successfully")
    void updateAccountStatus_ShouldUpdateSuccessfully() {
        // Arrange
        User.AccountStatus newStatus = User.AccountStatus.SUSPENDED;
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(newStatus, user.getAccountStatus());
            return user;
        });

        // Act
        userService.updateAccountStatus(testUserId, newStatus);

        // Assert
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("isEmailAvailable - Should return true when email is available")
    void isEmailAvailable_ShouldReturnTrue_WhenAvailable() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act
        boolean result = userService.isEmailAvailable("newemail@example.com");

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsByEmail(anyString());
    }

    @Test
    @DisplayName("isPhoneAvailable - Should return true when phone is available")
    void isPhoneAvailable_ShouldReturnTrue_WhenAvailable() {
        // Arrange
        when(userRepository.existsByPhone(anyString())).thenReturn(false);

        // Act
        boolean result = userService.isPhoneAvailable("+9999999999");

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsByPhone(anyString());
    }

    // ===== Negative Test Cases =====

    @Test
    @DisplayName("getUserById - Should throw UserNotFoundException when user does not exist")
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findActiveById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(testUserId));
        verify(userRepository, times(1)).findActiveById(testUserId);
    }

    @Test
    @DisplayName("getUserByEmail - Should throw UserNotFoundException when user does not exist")
    void getUserByEmail_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail("nonexistent@example.com"));
        verify(userRepository, times(1)).findActiveByEmail(anyString());
    }

    @Test
    @DisplayName("updateUser - Should throw UserNotFoundException when user does not exist")
    void updateUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.updateUser(testUserId, request));
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Should throw exception when phone is already taken")
    void updateUser_ShouldThrowException_WhenPhoneTaken() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("+9876543210");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(testUserId, request));
        assertEquals("Phone number is already in use", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deleteUser - Should throw UserNotFoundException when user does not exist")
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(testUserId));
        verify(userRepository, times(1)).findById(testUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("enableTwoFactorAuth - Should throw UserNotFoundException when user does not exist")
    void enableTwoFactorAuth_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.enableTwoFactorAuth(testUserId));
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("disableTwoFactorAuth - Should throw UserNotFoundException when user does not exist")
    void disableTwoFactorAuth_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.disableTwoFactorAuth(testUserId));
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("updateAccountStatus - Should throw UserNotFoundException when user does not exist")
    void updateAccountStatus_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> userService.updateAccountStatus(testUserId, User.AccountStatus.SUSPENDED));
        verify(userRepository, times(1)).findById(testUserId);
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("updateUser - Should not update when no fields provided")
    void updateUser_ShouldNotUpdateFields_WhenEmpty() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        // All fields are null

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.updateUser(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser - Should not reset phone verification when phone unchanged")
    void updateUser_ShouldKeepPhoneVerification_WhenPhoneUnchanged() {
        // Arrange
        testUser.setPhoneVerified(true);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone(testPhone); // Same phone

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertTrue(user.getPhoneVerified(), "Phone verification should remain unchanged");
            return user;
        });

        // Act
        UserResponse result = userService.updateUser(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(userRepository, never()).existsByPhone(anyString());
    }

    @Test
    @DisplayName("isEmailAvailable - Should return false when email is taken")
    void isEmailAvailable_ShouldReturnFalse_WhenTaken() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act
        boolean result = userService.isEmailAvailable(testEmail);

        // Assert
        assertFalse(result);
        verify(userRepository, times(1)).existsByEmail(anyString());
    }

    @Test
    @DisplayName("isPhoneAvailable - Should return false when phone is taken")
    void isPhoneAvailable_ShouldReturnFalse_WhenTaken() {
        // Arrange
        when(userRepository.existsByPhone(anyString())).thenReturn(true);

        // Act
        boolean result = userService.isPhoneAvailable(testPhone);

        // Assert
        assertFalse(result);
        verify(userRepository, times(1)).existsByPhone(anyString());
    }

    @Test
    @DisplayName("getUserByEmail - Should handle case-insensitive email")
    void getUserByEmail_ShouldHandleCaseInsensitive() {
        // Arrange
        String mixedCaseEmail = "TeSt@ExAmPlE.com";
        when(userRepository.findActiveByEmail(mixedCaseEmail.toLowerCase())).thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = userService.getUserByEmail(mixedCaseEmail);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).findActiveByEmail(mixedCaseEmail.toLowerCase());
    }

    @Test
    @DisplayName("getAllUsers - Should return empty page when no users exist")
    void getAllUsers_ShouldReturnEmptyPage_WhenNoUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = new PageImpl<>(Arrays.asList());

        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<UserResponse> result = userService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        verify(userRepository, times(1)).findAll(pageable);
    }
}
