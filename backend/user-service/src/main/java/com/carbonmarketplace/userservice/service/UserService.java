package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.dto.request.UpdateUserRequest;
import com.carbonmarketplace.userservice.dto.response.UserResponse;
import com.carbonmarketplace.userservice.entity.User;
import com.carbonmarketplace.userservice.exception.UserNotFoundException;
import com.carbonmarketplace.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserResponse.fromEntity(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findActiveByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserResponse.fromEntity(user);
    }

    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Update fields if provided
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }

        if (StringUtils.hasText(request.getPhone())) {
            // Check if phone is not already taken
            if (!request.getPhone().equals(user.getPhone()) &&
                    userRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("Phone number is already in use");
            }
            user.setPhone(request.getPhone());
            user.setPhoneVerified(false); // Reset verification status
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Update corporate buyer fields
        if (user.getRole() == User.UserRole.BUYER) {
            if (StringUtils.hasText(request.getCompanyName())) {
                user.setCompanyName(request.getCompanyName());
            }
            if (StringUtils.hasText(request.getTaxCode())) {
                user.setTaxCode(request.getTaxCode());
            }
            if (StringUtils.hasText(request.getBusinessRegistrationNumber())) {
                user.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
            }
        }

        User savedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return UserResponse.fromEntity(savedUser);
    }

    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Soft delete
        user.setDeletedAt(LocalDateTime.now());
        user.setAccountStatus(User.AccountStatus.CLOSED);

        userRepository.save(user);
        log.info("User soft deleted: {}", userId);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
    }

    public void verifyEmail(UUID userId) {
        userRepository.verifyEmail(userId);
        log.info("Email verified for user: {}", userId);
    }

    public void verifyPhone(UUID userId) {
        userRepository.verifyPhone(userId);
        log.info("Phone verified for user: {}", userId);
    }

    public String enableTwoFactorAuth(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Generate secret
        String secret = TotpService.generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);

        userRepository.save(user);
        log.info("2FA enabled for user: {}", userId);

        return TotpService.generateQRCodeUrl(user.getEmail(), secret);
    }

    public void disableTwoFactorAuth(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);

        userRepository.save(user);
        log.info("2FA disabled for user: {}", userId);
    }

    public void updateAccountStatus(UUID userId, User.AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setAccountStatus(status);
        userRepository.save(user);
        log.info("Account status updated to {} for user: {}", status, userId);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email.toLowerCase());
    }

    public boolean isPhoneAvailable(String phone) {
        return !userRepository.existsByPhone(phone);
    }
}
