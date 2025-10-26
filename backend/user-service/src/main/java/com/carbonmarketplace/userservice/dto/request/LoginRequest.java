package com.carbonmarketplace.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email or phone is required")
    private String username; // Can be email or phone

    @NotBlank(message = "Password is required")
    private String password;

    private String twoFactorCode; // Optional for 2FA

    private String deviceInfo; // Optional device information

    private String ipAddress; // Will be extracted from request
}
