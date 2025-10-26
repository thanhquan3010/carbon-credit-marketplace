package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.dto.request.LoginRequest;
import com.carbonmarketplace.userservice.dto.request.RefreshTokenRequest;
import com.carbonmarketplace.userservice.dto.request.RegisterRequest;
import com.carbonmarketplace.userservice.dto.response.AuthResponse;
import com.carbonmarketplace.userservice.dto.response.UserResponse;
import com.carbonmarketplace.userservice.entity.RefreshToken;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.exception.*;
import com.carbonmarketplace.userservice.repository.RefreshTokenRepository;
import com.carbonmarketplace.userservice.repository.UserRepository;
import com.carbonmarketplace.userservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final TotpService totpService;

    @Value("${spring.security.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 30 * 60 * 1000; // 30 minutes

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        // Check if phone already exists
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new PhoneAlreadyExistsException("Phone number is already registered");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.UserRole.valueOf(request.getRole().toUpperCase()))
                .kycLevel(0)
                .kycStatus(User.KycStatus.PENDING)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .companyName(request.getCompanyName())
                .taxCode(request.getTaxCode())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .lastPasswordChange(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        // Send verification email
        emailService.sendVerificationEmail(savedUser);

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String refreshTokenStr = tokenProvider.generateRefreshToken(savedUser);

        // Save refresh token
        saveRefreshToken(savedUser, refreshTokenStr, null, null);

        return AuthResponse.success(
                accessToken,
                refreshTokenStr,
                tokenProvider.getExpirationTime(),
                UserResponse.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getUsername());

        // Find user by email or phone
        User user = userRepository.findByEmailOrPhone(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException("Account is locked. Please try again later.");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            // Check 2FA if enabled
            if (user.getTwoFactorEnabled()) {
                if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty()) {
                    return AuthResponse.requiresTwoFactor();
                }

                if (!totpService.validateCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
                    throw new InvalidTwoFactorCodeException("Invalid 2FA code");
                }
            }

            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshTokenStr = tokenProvider.generateRefreshToken(user);

            // Save refresh token
            saveRefreshToken(user, refreshTokenStr, request.getDeviceInfo(), request.getIpAddress());

            log.info("Login successful for user: {}", user.getUserId());

            return AuthResponse.success(
                    accessToken,
                    refreshTokenStr,
                    tokenProvider.getExpirationTime(),
                    UserResponse.fromEntity(user));

        } catch (BadCredentialsException e) {
            // Increment failed attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                userRepository.save(user);
                throw new AccountLockedException("Account has been locked due to too many failed login attempts");
            }

            userRepository.save(user);
            throw new BadCredentialsException(
                    "Invalid credentials. " + (MAX_LOGIN_ATTEMPTS - attempts) + " attempts remaining");
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Validate refresh token
        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();

        // Generate new access token
        String accessToken = tokenProvider.generateAccessToken(user);

        // Optionally generate new refresh token (rotating refresh tokens)
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        // Revoke old refresh token
        refreshTokenRepository.revokeToken(refreshToken.getToken(), LocalDateTime.now());

        // Save new refresh token
        saveRefreshToken(user, newRefreshToken, request.getDeviceInfo(), null);

        return AuthResponse.success(
                accessToken,
                newRefreshToken,
                tokenProvider.getExpirationTime(),
                UserResponse.fromEntity(user));
    }

    public void logout(String refreshToken) {
        log.info("Logging out user");

        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(token -> {
                        refreshTokenRepository.revokeToken(token.getToken(), LocalDateTime.now());
                        log.info("Refresh token revoked for user: {}", token.getUser().getUserId());
                    });
        }

        SecurityContextHolder.clearContext();
    }

    public void logoutAll(UUID userId) {
        log.info("Logging out all sessions for user: {}", userId);
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        SecurityContextHolder.clearContext();
    }

    private void saveRefreshToken(User user, String token, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(user.getUserId(), LocalDateTime.now());

        log.info("Password reset successful for user: {}", user.getUserId());
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());

        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());

        log.info("Password changed successfully for user: {}", userId);
    }
}
