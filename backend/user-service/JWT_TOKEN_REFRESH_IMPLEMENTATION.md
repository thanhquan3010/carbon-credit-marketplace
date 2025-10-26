# JWT Token Refresh Implementation

## Overview

This document describes the implementation of automatic JWT token refresh with rotation to solve the common pitfall of token expiry during long operations.

## Problem Statement

**Issue**: JWT tokens expire during long operations, causing authentication failures and poor user experience.

**Solution**: Implement automatic token refresh before expiry (when < 60 seconds remaining) with refresh token rotation for enhanced security.

## Implementation Components

### Backend Components

#### 1. JwtTokenProvider Enhancements

**File**: `src/main/java/com/carbonmarketplace/userservice/security/JwtTokenProvider.java`

**New Methods**:
- `getRemainingTimeInSeconds(String token)` - Calculate remaining time until token expires
- `isTokenExpiringSoon(String token, long thresholdSeconds)` - Check if token expires within threshold
- `shouldRefreshToken(String token)` - Check if token should be refreshed (< 60 seconds)

**Usage Example**:
```java
String token = "eyJhbGciOiJIUzI1...";
long remainingTime = tokenProvider.getRemainingTimeInSeconds(token);

if (tokenProvider.shouldRefreshToken(token)) {
    // Token expires in < 60 seconds, refresh now
    String newToken = authService.refreshToken(refreshTokenRequest);
}
```

#### 2. TokenExpirationResponse DTO

**File**: `src/main/java/com/carbonmarketplace/userservice/dto/response/TokenExpirationResponse.java`

**Fields**:
- `expiresIn` - Seconds until token expires
- `expired` - Whether token is already expired
- `shouldRefresh` - Whether token should be refreshed
- `issuedAt` - Token issue timestamp
- `expiresAt` - Token expiration timestamp

#### 3. Token Status Endpoint

**Endpoint**: `GET /auth/token/status`

**Headers**: 
```
Authorization: Bearer {access_token}
```

**Response**:
```json
{
    "success": true,
    "message": "Token status retrieved",
    "data": {
        "expiresIn": 450,
        "expired": false,
        "shouldRefresh": false,
        "issuedAt": 1698345600,
        "expiresAt": 1698346500
    }
}
```

#### 4. Refresh Token Service

**File**: `src/main/java/com/carbonmarketplace/userservice/service/AuthService.java`

**Method**: `checkTokenExpiration(String token)`

Validates token and returns expiration information.

#### 5. Scheduled Token Cleanup

**File**: `src/main/java/com/carbonmarketplace/userservice/service/RefreshTokenCleanupService.java`

**Scheduled Tasks**:
- **Daily at 2 AM**: Clean up expired and old revoked tokens (7-day retention)
- **Every hour**: Log token statistics for monitoring

**Manual Cleanup**:
```java
@Autowired
private RefreshTokenCleanupService cleanupService;

int deletedCount = cleanupService.performManualCleanup();
```

### Frontend Components

#### 1. Token Manager Service

**File**: `src/services/tokenManager.ts`

**Key Features**:
- Stores tokens with expiration tracking in localStorage
- Monitors token expiration every 30 seconds
- Automatically refreshes tokens when < 60 seconds remaining
- Prevents multiple simultaneous refresh requests (singleton pattern)

**Usage Example**:
```typescript
import { tokenManager } from './services/tokenManager';

// Store tokens after login
tokenManager.setTokens(accessToken, refreshToken, expiresIn);

// Check if refresh needed
if (tokenManager.shouldRefreshToken()) {
    await tokenManager.refreshTokenIfNeeded();
}

// Get token status
const status = tokenManager.getTokenStatus();
console.log('Token expires in:', status.expiresIn, 'seconds');

// Manual cleanup
tokenManager.clearTokens();
```

#### 2. API Interceptor

**File**: `src/services/api.ts`

**Features**:
- **Request Interceptor**: Proactively refreshes token before each API call if needed
- **Response Interceptor**: Handles 401 errors as fallback, attempts token refresh and retries request
- **Smart Routing**: Skips refresh for auth endpoints to avoid infinite loops

**How it Works**:
```typescript
// Before each request
1. Check if token expires in < 60 seconds
2. If yes, refresh token proactively
3. Add updated token to request headers

// On 401 error response
1. Attempt token refresh
2. Retry original request with new token
3. If refresh fails, redirect to login
```

#### 3. React Hooks

**File**: `src/hooks/useTokenStatus.ts`

**Available Hooks**:

**useTokenStatus()**:
```tsx
const { 
    hasToken, 
    expiresIn, 
    shouldRefresh, 
    isExpired,
    expiresInMinutes,
    expiresInSeconds 
} = useTokenStatus();
```

**useTokenExpirationText()**:
```tsx
const expirationText = useTokenExpirationText();
// Returns: "Expires in 14m 32s" or "Expired" or "No token"
```

#### 4. Token Status Indicator Component

**File**: `src/components/common/TokenStatusIndicator.tsx`

**Usage**:
```tsx
// Simple indicator (dot)
<TokenStatusIndicator />

// Detailed view
<TokenStatusIndicator showDetails={true} />
```

**Color Coding**:
- 🟢 Green: > 5 minutes remaining
- 🟠 Orange: < 5 minutes remaining
- 🟡 Yellow: Refreshing (< 60 seconds)
- 🔴 Red: Expired

## Configuration

### Backend Configuration

**File**: `src/main/resources/application.yml`

```yaml
spring:
  security:
    jwt:
      secret: ${JWT_SECRET:your-256-bit-secret-key-here}
      expiration: 900000  # 15 minutes in milliseconds
      refresh-expiration: 86400000  # 24 hours in milliseconds
```

### Frontend Configuration

**File**: `.env.local`

```properties
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Security Features

### 1. Refresh Token Rotation

Every time a refresh token is used, it is:
1. Validated
2. Revoked immediately
3. Replaced with a new refresh token

This prevents:
- Token replay attacks
- Compromised refresh tokens from being reused

### 2. Token Expiration Tracking

- Access tokens: 15 minutes (configurable)
- Refresh tokens: 24 hours (configurable)
- Expired tokens are cleaned up after 7 days

### 3. Concurrent Refresh Prevention

The frontend uses a singleton promise pattern to ensure only one refresh request is made at a time, even with multiple simultaneous API calls.

## Testing

### Backend Tests

```java
@Test
public void testTokenExpirationCheck() {
    String token = tokenProvider.generateAccessToken(user);
    
    // Token should not need refresh immediately
    assertFalse(tokenProvider.shouldRefreshToken(token));
    
    // Remaining time should be close to expiration time
    long remaining = tokenProvider.getRemainingTimeInSeconds(token);
    assertTrue(remaining > 0 && remaining <= 900);
}

@Test
public void testRefreshTokenRotation() {
    AuthResponse response = authService.login(loginRequest);
    String oldRefreshToken = response.getRefreshToken();
    
    // Refresh token
    RefreshTokenRequest refreshRequest = new RefreshTokenRequest(oldRefreshToken);
    AuthResponse newResponse = authService.refreshToken(refreshRequest);
    
    // Old token should be revoked
    assertThrows(InvalidTokenException.class, () -> {
        authService.refreshToken(refreshRequest);
    });
    
    // New token should work
    assertNotNull(newResponse.getAccessToken());
}
```

### Frontend Tests

```typescript
describe('TokenManager', () => {
    test('should refresh token when expiring soon', async () => {
        tokenManager.setTokens('old-token', 'refresh-token', 30); // Expires in 30s
        
        expect(tokenManager.shouldRefreshToken()).toBe(true);
        
        const newToken = await tokenManager.refreshTokenIfNeeded();
        expect(newToken).not.toBe('old-token');
    });
    
    test('should prevent multiple simultaneous refreshes', async () => {
        tokenManager.setTokens('token', 'refresh-token', 30);
        
        const promise1 = tokenManager.refreshTokenIfNeeded();
        const promise2 = tokenManager.refreshTokenIfNeeded();
        
        // Both should return the same promise
        expect(promise1).toBe(promise2);
    });
});
```

## Monitoring and Debugging

### Backend Logs

```
[TokenManager] Token expires in 45 seconds
[TokenManager] Auto-refreshing token (remaining: 45s)
[TokenManager] Token refreshed successfully
```

### Frontend Console Logs

```
[TokenManager] Tokens stored. Expires at: 3:45:00 PM
[TokenManager] Token monitoring started
[API] Token refresh failed in request interceptor: Error
[TokenManager] Auto-refresh failed: Invalid refresh token
```

### Token Status Check

```bash
# Check token status via API
curl -H "Authorization: Bearer {token}" \
     http://localhost:8081/auth/token/status
```

## Deployment Checklist

- [ ] Set `JWT_SECRET` environment variable (production secret)
- [ ] Configure appropriate token expiration times
- [ ] Enable Spring scheduling (@EnableScheduling)
- [ ] Set up monitoring for token refresh failures
- [ ] Configure alerts for high token refresh rates
- [ ] Test token rotation in production-like environment
- [ ] Verify cleanup job runs successfully
- [ ] Document token management policies

## Troubleshooting

### Issue: Tokens not refreshing automatically

**Check**:
1. Is `@EnableScheduling` added to application class?
2. Are frontend interceptors properly configured?
3. Check browser console for errors
4. Verify API endpoint is accessible

### Issue: Multiple refresh requests

**Solution**: The singleton pattern should prevent this. Check that `tokenManager.getInstance()` is being used consistently.

### Issue: Tokens expiring too quickly

**Solution**: Adjust `spring.security.jwt.expiration` in backend configuration.

### Issue: Old tokens not being cleaned up

**Check**: 
1. Verify `@Scheduled` annotation is present
2. Check application logs for cleanup job execution
3. Manually trigger cleanup via service method

## Best Practices

1. **Never store tokens in cookies without HttpOnly flag**
2. **Always use HTTPS in production**
3. **Rotate secrets regularly**
4. **Monitor token refresh rates for anomalies**
5. **Set reasonable expiration times** (15 min for access, 24h for refresh)
6. **Revoke all tokens on password change**
7. **Implement rate limiting on refresh endpoint**
8. **Log suspicious refresh patterns**

## Performance Impact

- **Backend**: Minimal overhead (~5ms per token check)
- **Frontend**: Background checks every 30 seconds (negligible)
- **Storage**: Refresh tokens stored in database (indexed)
- **Network**: One extra refresh call per session when needed

## Future Enhancements

1. **Redis caching** for revoked tokens (faster lookup)
2. **Device fingerprinting** for enhanced security
3. **Suspicious activity detection** (multiple refreshes from different IPs)
4. **Token sliding window** (extend expiration on activity)
5. **Refresh token family tracking** (detect token theft)

---

**Document Version**: 1.0  
**Last Updated**: October 26, 2025  
**Author**: Carbon Credit Marketplace Team

