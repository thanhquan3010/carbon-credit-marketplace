package com.carbonmarketplace.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // in seconds
    private UserResponse user;
    private Boolean requiresTwoFactor;
    private String message;
    private LocalDateTime issuedAt;

    public static AuthResponse requiresTwoFactor() {
        return AuthResponse.builder()
                .requiresTwoFactor(true)
                .message("Two-factor authentication required")
                .build();
    }

    public static AuthResponse success(String accessToken, String refreshToken, Long expiresIn, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .requiresTwoFactor(false)
                .issuedAt(LocalDateTime.now())
                .build();
    }
}
