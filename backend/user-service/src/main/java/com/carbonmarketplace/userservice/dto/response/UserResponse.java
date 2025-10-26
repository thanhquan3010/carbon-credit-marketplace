package com.carbonmarketplace.userservice.dto.response;

import com.carbonmarketplace.userservice.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID userId;
    private String email;
    private String phone;
    private String fullName;
    private String role;

    // KYC Information
    private Integer kycLevel;
    private String kycStatus;

    // Account Status
    private String accountStatus;
    private Boolean emailVerified;
    private Boolean phoneVerified;

    // Profile
    private String avatarUrl;
    private String bio;
    private String companyName;
    private String taxCode;

    // Security
    private Boolean twoFactorEnabled;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .kycLevel(user.getKycLevel())
                .kycStatus(user.getKycStatus().name())
                .accountStatus(user.getAccountStatus().name())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .companyName(user.getCompanyName())
                .taxCode(user.getTaxCode())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
