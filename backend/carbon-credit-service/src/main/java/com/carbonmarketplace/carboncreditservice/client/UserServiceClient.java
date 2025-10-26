package com.carbonmarketplace.carboncreditservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for communicating with User Service
 */
@FeignClient(name = "user-service", url = "${feign.client.config.user-service.url}")
public interface UserServiceClient {
    
    @GetMapping("/api/v1/users/{userId}")
    UserDto getUserById(@PathVariable UUID userId);
    
    @GetMapping("/api/v1/users/{userId}/kyc-status")
    KycStatusDto getUserKycStatus(@PathVariable UUID userId);
}

/**
 * User DTO
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class UserDto {
    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private String kycStatus;
    private Integer kycLevel;
}

/**
 * KYC Status DTO
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class KycStatusDto {
    private UUID userId;
    private String status;
    private Integer level;
    private Boolean isVerified;
}
