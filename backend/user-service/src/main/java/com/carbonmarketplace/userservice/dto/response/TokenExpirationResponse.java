package com.carbonmarketplace.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenExpirationResponse {

    private Long expiresIn; // Seconds until token expires
    private Boolean expired; // Whether token is already expired
    private Boolean shouldRefresh; // Whether token should be refreshed (< 60s remaining)
    private Long issuedAt; // Token issue timestamp
    private Long expiresAt; // Token expiration timestamp

    public static TokenExpirationResponse from(long remainingSeconds, boolean expired, boolean shouldRefresh,
            long issuedAt, long expiresAt) {
        return TokenExpirationResponse.builder()
                .expiresIn(remainingSeconds)
                .expired(expired)
                .shouldRefresh(shouldRefresh)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }
}
