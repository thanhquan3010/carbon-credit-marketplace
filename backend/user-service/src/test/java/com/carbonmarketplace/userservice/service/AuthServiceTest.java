package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.dto.request.LoginRequest;
import com.carbonmarketplace.userservice.dto.request.RefreshTokenRequest;
import com.carbonmarketplace.userservice.dto.request.RegisterRequest;
import com.carbonmarketplace.userservice.dto.response.AuthResponse;
import com.carbonmarketplace.userservice.entity.RefreshToken;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.exception.AccountLockedException;
import com.carbonmarketplace.userservice.exception.EmailAlreadyExistsException;
import com.carbonmarketplace.userservice.exception.InvalidTokenException;
import com.carbonmarketplace.userservice.exception.InvalidTwoFactorCodeException;
import com.carbonmarketplace.userservice.exception.PhoneAlreadyExistsException;
import com.carbonmarketplace.userservice.exception.UserNotFoundException;
import com.carbonmarketplace.userservice.repository.RefreshTokenRepository;
import com.carbonmarketplace.userservice.repository.UserRepository;
import com.carbonmarketplace.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailService emailService;

    @Mock
    private TotpService totpService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private UUID testUserId;
    private String testEmail;
    private String testPassword;
    private String accessToken;
    private String refreshTokenString;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        testPassword = "Password123!";
        accessToken = "jwt.access.token";
        refreshTokenString = "jwt.refresh.token";

        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L); // 7 days

        registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);
        registerRequest.setFullName("Test User");
        registerRequest.setRole("EVOWNER");
        registerRequest.setPhone("+1234567890");

        loginRequest = new LoginRequest();
        loginRequest.setUsername(testEmail);
        loginRequest.setPassword(testPassword);

        testUser = User.builder()
                .userId(testUserId)
                .email(testEmail)
                .phone("+1234567890")
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .role(User.UserRole.EVOWNER)
                .kycLevel(0)
                .kycStatus(User.KycStatus.PENDING)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .build();
    }

    // ===== Registration Tests =====

    @Test
    @DisplayName("register - Should register new user successfully")
    void register_ShouldRegisterSuccessfully_WithValidData() {
        // Arrange
        when(userRepository.existsByEmail(testEmail.toLowerCase())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequest.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessToken(any(User.class))).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(any(User.class))).thenReturn(refreshTokenString);
        when(tokenProvider.getExpirationTime()).thenReturn(3600000L);
        doNothing().when(emailService).sendVerificationEmail(any(User.class));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals(accessToken, result.getAccessToken());
        assertEquals(refreshTokenString, result.getRefreshToken());
        assertNotNull(result.getUser());
        verify(userRepository, times(1)).existsByEmail(testEmail.toLowerCase());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register - Should throw exception when email already exists")
    void register_ShouldThrowException_WhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(testEmail.toLowerCase())).thenReturn(true);

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(registerRequest));
        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail(testEmail.toLowerCase());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register - Should throw exception when phone already exists")
    void register_ShouldThrowException_WhenPhoneExists() {
        // Arrange
        when(userRepository.existsByEmail(testEmail.toLowerCase())).thenReturn(false);
        when(userRepository.existsByPhone(registerRequest.getPhone())).thenReturn(true);

        // Act & Assert
        PhoneAlreadyExistsException exception = assertThrows(PhoneAlreadyExistsException.class,
                () -> authService.register(registerRequest));
        assertEquals("Phone number is already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register - Should handle registration without phone")
    void register_ShouldRegisterSuccessfully_WithoutPhone() {
        // Arrange
        registerRequest.setPhone(null);
        when(userRepository.existsByEmail(testEmail.toLowerCase())).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessToken(any(User.class))).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(any(User.class))).thenReturn(refreshTokenString);
        when(tokenProvider.getExpirationTime()).thenReturn(3600000L);
        doNothing().when(emailService).sendVerificationEmail(any(User.class));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository, never()).existsByPhone(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ===== Login Tests =====

    @Test
    @DisplayName("login - Should login successfully with valid credentials")
    void login_ShouldLoginSuccessfully_WithValidCredentials() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateAccessToken(testUser)).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn(refreshTokenString);
        when(tokenProvider.getExpirationTime()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals(accessToken, result.getAccessToken());
        assertEquals(refreshTokenString, result.getRefreshToken());
        assertNotNull(result.getUser());
        verify(userRepository, times(1)).findByEmailOrPhone(testEmail, testEmail);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login - Should require 2FA when enabled")
    void login_ShouldRequireTwoFactor_When2FAEnabled() {
        // Arrange
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("SECRET123");
        loginRequest.setTwoFactorCode(null);

        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.getRequiresTwoFactor());
        assertNull(result.getAccessToken());
        verify(totpService, never()).validateCode(anyString(), anyString());
    }

    @Test
    @DisplayName("login - Should login successfully with valid 2FA code")
    void login_ShouldLoginSuccessfully_WithValid2FACode() {
        // Arrange
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("SECRET123");
        loginRequest.setTwoFactorCode("123456");

        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(totpService.validateCode("SECRET123", "123456")).thenReturn(true);
        when(tokenProvider.generateAccessToken(testUser)).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn(refreshTokenString);
        when(tokenProvider.getExpirationTime()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals(accessToken, result.getAccessToken());
        verify(totpService, times(1)).validateCode("SECRET123", "123456");
    }

    @Test
    @DisplayName("login - Should throw exception with invalid 2FA code")
    void login_ShouldThrowException_WithInvalid2FACode() {
        // Arrange
        testUser.setTwoFactorEnabled(true);
        testUser.setTwoFactorSecret("SECRET123");
        loginRequest.setTwoFactorCode("999999");

        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(totpService.validateCode("SECRET123", "999999")).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidTwoFactorCodeException.class, () -> authService.login(loginRequest));
        verify(totpService, times(1)).validateCode("SECRET123", "999999");
    }

    @Test
    @DisplayName("login - Should throw exception when user not found")
    void login_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("login - Should throw exception when account is locked")
    void login_ShouldThrowException_WhenAccountLocked() {
        // Arrange
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));

        // Act & Assert
        AccountLockedException exception = assertThrows(AccountLockedException.class,
                () -> authService.login(loginRequest));
        assertTrue(exception.getMessage().contains("locked"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("login - Should increment failed attempts on bad credentials")
    void login_ShouldIncrementFailedAttempts_OnBadCredentials() {
        // Arrange
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(1, user.getFailedLoginAttempts());
            return user;
        });

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("login - Should lock account after max failed attempts")
    void login_ShouldLockAccount_AfterMaxAttempts() {
        // Arrange
        testUser.setFailedLoginAttempts(4); // One more attempt will lock
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(5, user.getFailedLoginAttempts());
            assertNotNull(user.getLockedUntil());
            return user;
        });

        // Act & Assert
        AccountLockedException exception = assertThrows(AccountLockedException.class,
                () -> authService.login(loginRequest));
        assertTrue(exception.getMessage().contains("locked"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("login - Should reset failed attempts on successful login")
    void login_ShouldResetFailedAttempts_OnSuccess() {
        // Arrange
        testUser.setFailedLoginAttempts(3);
        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByEmailOrPhone(testEmail, testEmail)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateAccessToken(testUser)).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn(refreshTokenString);
        when(tokenProvider.getExpirationTime()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(0, user.getFailedLoginAttempts());
            return user;
        });
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ===== Refresh Token Tests =====

    @Test
    @DisplayName("refreshToken - Should refresh token successfully")
    void refreshToken_ShouldRefreshSuccessfully_WithValidToken() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshTokenString);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(Optional.of(refreshToken));
        when(tokenProvider.generateAccessToken(testUser)).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("new.refresh.token");
        when(tokenProvider.getExpirationTime()).thenReturn(3600000L);
        doNothing().when(refreshTokenRepository).revokeToken(refreshTokenString, any(LocalDateTime.class));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse result = authService.refreshToken(request);

        // Assert
        assertNotNull(result);
        assertEquals(accessToken, result.getAccessToken());
        assertEquals("new.refresh.token", result.getRefreshToken());
        verify(refreshTokenRepository, times(1)).revokeToken(eq(refreshTokenString), any(LocalDateTime.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refreshToken - Should throw exception with invalid token")
    void refreshToken_ShouldThrowException_WithInvalidToken() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid.token");

        when(refreshTokenRepository.findByToken("invalid.token")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidTokenException exception = assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(request));
        assertEquals("Invalid refresh token", exception.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refreshToken - Should throw exception with expired token")
    void refreshToken_ShouldThrowException_WithExpiredToken() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshTokenString);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        InvalidTokenException exception = assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(request));
        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("refreshToken - Should throw exception with revoked token")
    void refreshToken_ShouldThrowException_WithRevokedToken() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshTokenString);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true) // Revoked
                .revokedAt(LocalDateTime.now())
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        InvalidTokenException exception = assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(request));
        assertTrue(exception.getMessage().contains("revoked"));
    }

    // ===== Logout Tests =====

    @Test
    @DisplayName("logout - Should logout successfully with refresh token")
    void logout_ShouldLogoutSuccessfully_WithRefreshToken() {
        // Arrange
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(testUser)
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(Optional.of(refreshToken));
        doNothing().when(refreshTokenRepository).revokeToken(eq(refreshTokenString), any(LocalDateTime.class));

        // Act
        authService.logout(refreshTokenString);

        // Assert
        verify(refreshTokenRepository, times(1)).findByToken(refreshTokenString);
        verify(refreshTokenRepository, times(1)).revokeToken(eq(refreshTokenString), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("logout - Should handle logout without refresh token")
    void logout_ShouldHandleLogout_WithoutRefreshToken() {
        // Act
        authService.logout(null);

        // Assert
        verify(refreshTokenRepository, never()).findByToken(anyString());
        verify(refreshTokenRepository, never()).revokeToken(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("logoutAll - Should revoke all user tokens")
    void logoutAll_ShouldRevokeAllTokens() {
        // Arrange
        doNothing().when(refreshTokenRepository).revokeAllUserTokens(eq(testUserId), any(LocalDateTime.class));

        // Act
        authService.logoutAll(testUserId);

        // Assert
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(eq(testUserId), any(LocalDateTime.class));
    }

    // ===== Password Management Tests =====

    @Test
    @DisplayName("resetPassword - Should reset password successfully")
    void resetPassword_ShouldResetSuccessfully() {
        // Arrange
        String newPassword = "NewPassword123!";
        when(userRepository.findByEmail(testEmail.toLowerCase())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("newHashedPassword", user.getPasswordHash());
            assertEquals(0, user.getFailedLoginAttempts());
            assertNull(user.getLockedUntil());
            return user;
        });
        doNothing().when(refreshTokenRepository).revokeAllUserTokens(eq(testUserId), any(LocalDateTime.class));

        // Act
        authService.resetPassword(testEmail, newPassword);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(eq(testUserId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("resetPassword - Should throw exception when user not found")
    void resetPassword_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(testEmail.toLowerCase())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> authService.resetPassword(testEmail, "newPassword"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword - Should change password successfully")
    void changePassword_ShouldChangeSuccessfully_WithCorrectCurrentPassword() {
        // Arrange
        String currentPassword = "OldPassword123!";
        String newPassword = "NewPassword123!";

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(refreshTokenRepository).revokeAllUserTokens(eq(testUserId), any(LocalDateTime.class));

        // Act
        authService.changePassword(testUserId, currentPassword, newPassword);

        // Assert
        verify(passwordEncoder, times(1)).matches(currentPassword, testUser.getPasswordHash());
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(eq(testUserId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("changePassword - Should throw exception with incorrect current password")
    void changePassword_ShouldThrowException_WithIncorrectCurrentPassword() {
        // Arrange
        String currentPassword = "WrongPassword123!";
        String newPassword = "NewPassword123!";

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.changePassword(testUserId, currentPassword, newPassword));
        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword - Should throw exception when user not found")
    void changePassword_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> authService.changePassword(testUserId, "oldPass", "newPass"));
        verify(userRepository, never()).save(any(User.class));
    }
}
