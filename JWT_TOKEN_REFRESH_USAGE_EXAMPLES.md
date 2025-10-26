# JWT Token Refresh - Usage Examples

## Overview

This document provides practical examples of using the JWT token refresh functionality in the Carbon Credit Marketplace application.

---

## Backend Examples

### Example 1: Check Token Status in Controller

```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        
        // Check if token is expiring soon
        if (tokenProvider.shouldRefreshToken(token)) {
            // Warn client to refresh
            log.warn("Client token expiring soon");
        }
        
        // Get remaining time for logging
        long remainingTime = tokenProvider.getRemainingTimeInSeconds(token);
        log.debug("Token valid for {} more seconds", remainingTime);
        
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
```

### Example 2: Admin Endpoint for Token Cleanup

```java
@RestController
@RequestMapping("/admin/tokens")
@PreAuthorize("hasRole('ADMIN')")
public class TokenAdminController {
    
    @Autowired
    private RefreshTokenCleanupService cleanupService;
    
    @Autowired
    private RefreshTokenRepository tokenRepository;
    
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> manualCleanup() {
        int deleted = cleanupService.performManualCleanup();
        long remaining = tokenRepository.count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedTokens", deleted);
        result.put("remainingTokens", remaining);
        result.put("cleanupTime", LocalDateTime.now());
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTokenStats() {
        LocalDateTime now = LocalDateTime.now();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTokens", tokenRepository.count());
        stats.put("activeTokens", tokenRepository.countActiveTokensByUserId(userId, now));
        
        return ResponseEntity.ok(stats);
    }
}
```

### Example 3: Custom Security Filter with Token Check

```java
@Component
public class TokenValidationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (token != null && tokenProvider.validateToken(token)) {
            // Check if token is expiring soon (< 60 seconds)
            if (tokenProvider.shouldRefreshToken(token)) {
                // Add warning header
                response.addHeader("X-Token-Expiring", "true");
                response.addHeader("X-Token-Expires-In", 
                    String.valueOf(tokenProvider.getRemainingTimeInSeconds(token)));
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## Frontend Examples

### Example 1: Basic React Component with Token Status

```tsx
import React from 'react';
import { useTokenStatus } from '../hooks/useTokenStatus';
import { TokenStatusIndicator } from '../components/common/TokenStatusIndicator';

export const UserDashboard: React.FC = () => {
    const tokenStatus = useTokenStatus();
    
    return (
        <div>
            <header className="flex justify-between items-center p-4">
                <h1>Dashboard</h1>
                
                {/* Show token status indicator */}
                <TokenStatusIndicator showDetails={true} />
            </header>
            
            {tokenStatus.shouldRefresh && (
                <div className="bg-yellow-100 p-3 rounded">
                    ⚠️ Your session will expire in {tokenStatus.expiresInMinutes} minutes
                </div>
            )}
            
            <main>
                {/* Dashboard content */}
            </main>
        </div>
    );
};
```

### Example 2: Token Status in Development Tools

```tsx
import React, { useState } from 'react';
import { tokenManager } from '../services/tokenManager';
import { useTokenStatus } from '../hooks/useTokenStatus';

export const DevToolsPanel: React.FC = () => {
    const status = useTokenStatus();
    const [refreshing, setRefreshing] = useState(false);
    
    const handleManualRefresh = async () => {
        setRefreshing(true);
        try {
            await tokenManager.refreshTokenIfNeeded();
            alert('Token refreshed successfully!');
        } catch (error) {
            alert('Refresh failed: ' + error.message);
        } finally {
            setRefreshing(false);
        }
    };
    
    const handleClearTokens = () => {
        if (confirm('Clear all tokens? You will be logged out.')) {
            tokenManager.clearTokens();
            window.location.href = '/auth/login';
        }
    };
    
    return (
        <div className="fixed bottom-4 right-4 bg-white shadow-lg rounded-lg p-4 border">
            <h3 className="font-bold mb-2">Token Status (Dev Tools)</h3>
            
            <div className="space-y-2 text-sm">
                <div>
                    <strong>Has Token:</strong> {status.hasToken ? '✅' : '❌'}
                </div>
                <div>
                    <strong>Expires In:</strong> {status.expiresInMinutes}m {status.expiresInSeconds}s
                </div>
                <div>
                    <strong>Should Refresh:</strong> {status.shouldRefresh ? '⚠️ Yes' : '✅ No'}
                </div>
                <div>
                    <strong>Is Expired:</strong> {status.isExpired ? '❌ Yes' : '✅ No'}
                </div>
            </div>
            
            <div className="mt-4 space-y-2">
                <button
                    onClick={handleManualRefresh}
                    disabled={refreshing}
                    className="w-full bg-blue-500 text-white px-3 py-1 rounded"
                >
                    {refreshing ? 'Refreshing...' : 'Manual Refresh'}
                </button>
                
                <button
                    onClick={handleClearTokens}
                    className="w-full bg-red-500 text-white px-3 py-1 rounded"
                >
                    Clear Tokens
                </button>
            </div>
        </div>
    );
};
```

### Example 3: Protected Route with Token Check

```tsx
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { tokenManager } from '../services/tokenManager';

interface ProtectedRouteProps {
    children: React.ReactNode;
    requireAuth?: boolean;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
    children, 
    requireAuth = true 
}) => {
    const router = useRouter();
    
    useEffect(() => {
        if (requireAuth) {
            const token = tokenManager.getToken();
            
            if (!token) {
                router.push('/auth/login');
                return;
            }
            
            // Check if token is expired
            if (tokenManager.isTokenExpired()) {
                tokenManager.clearTokens();
                router.push('/auth/login');
                return;
            }
            
            // Proactively refresh if needed
            tokenManager.refreshTokenIfNeeded().catch(() => {
                router.push('/auth/login');
            });
        }
    }, [requireAuth, router]);
    
    return <>{children}</>;
};

// Usage:
// <ProtectedRoute>
//   <Dashboard />
// </ProtectedRoute>
```

### Example 4: Login Component with Token Storage

```tsx
import React, { useState } from 'react';
import { authService } from '../services/authService';
import { useRouter } from 'next/router';

export const LoginForm: React.FC = () => {
    const router = useRouter();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    
    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        
        try {
            // authService automatically stores tokens via tokenManager
            const response = await authService.login(email, password);
            
            console.log('Login successful!');
            console.log('User:', response.user);
            
            // Token is automatically stored and monitoring started
            router.push('/dashboard');
            
        } catch (error) {
            alert('Login failed: ' + error.message);
        } finally {
            setLoading(false);
        }
    };
    
    return (
        <form onSubmit={handleLogin}>
            <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email"
                required
            />
            <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Password"
                required
            />
            <button type="submit" disabled={loading}>
                {loading ? 'Logging in...' : 'Login'}
            </button>
        </form>
    );
};
```

### Example 5: Custom API Call with Token Management

```tsx
import { apiClient } from '../services/api';
import { tokenManager } from '../services/tokenManager';

// The API client automatically handles token refresh
// But you can also check token status before making calls

export const fetchUserDashboard = async () => {
    // Optional: Check token status before important operations
    const status = tokenManager.getTokenStatus();
    
    if (status.isExpired) {
        throw new Error('Session expired. Please log in again.');
    }
    
    if (status.shouldRefresh) {
        console.log('Token expiring soon, will auto-refresh...');
        // Auto-refresh happens in API interceptor
    }
    
    // Make API call - token refresh happens automatically if needed
    try {
        const data = await apiClient.get('/dashboard/stats');
        return data;
    } catch (error) {
        console.error('API call failed:', error);
        throw error;
    }
};

// Usage in component:
// const data = await fetchUserDashboard();
```

### Example 6: Notification on Token Expiry

```tsx
import React, { useEffect } from 'react';
import { useTokenStatus } from '../hooks/useTokenStatus';
import { toast } from 'react-toastify';

export const TokenExpiryNotifier: React.FC = () => {
    const { expiresIn, shouldRefresh } = useTokenStatus();
    
    useEffect(() => {
        // Show warning at 5 minutes
        if (expiresIn > 0 && expiresIn <= 300 && expiresIn > 240) {
            toast.warning('Your session will expire in 5 minutes', {
                autoClose: 5000,
            });
        }
        
        // Show critical warning at 2 minutes
        if (expiresIn > 0 && expiresIn <= 120 && expiresIn > 60) {
            toast.error('Your session will expire in 2 minutes!', {
                autoClose: false,
            });
        }
        
        // Show refreshing message
        if (shouldRefresh) {
            toast.info('Refreshing your session...', {
                autoClose: 2000,
            });
        }
    }, [expiresIn, shouldRefresh]);
    
    return null; // This is a notification-only component
};

// Add to your main layout:
// <TokenExpiryNotifier />
```

---

## Testing Examples

### Backend Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
public class TokenRefreshIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Test
    public void testAutomaticTokenRefresh() throws Exception {
        // Login
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        
        String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), 
                                            "$.data.refreshToken");
        
        // Refresh token
        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        
        String newAccessToken = JsonPath.read(refreshResult.getResponse().getContentAsString(), 
                                              "$.data.accessToken");
        
        assertNotNull(newAccessToken);
        assertTrue(tokenProvider.validateToken(newAccessToken));
    }
}
```

### Frontend Unit Test

```typescript
import { tokenManager } from '../services/tokenManager';

describe('TokenManager', () => {
    beforeEach(() => {
        localStorage.clear();
    });
    
    test('should store and retrieve tokens', () => {
        tokenManager.setTokens('access-token', 'refresh-token', 900);
        
        expect(tokenManager.getToken()).toBe('access-token');
        expect(tokenManager.getRefreshToken()).toBe('refresh-token');
    });
    
    test('should calculate remaining time correctly', () => {
        const expiresIn = 600; // 10 minutes
        tokenManager.setTokens('token', 'refresh', expiresIn);
        
        const remaining = tokenManager.getTokenRemainingTime();
        expect(remaining).toBeGreaterThan(590);
        expect(remaining).toBeLessThanOrEqual(600);
    });
    
    test('should detect when token needs refresh', () => {
        // Token expiring in 30 seconds
        tokenManager.setTokens('token', 'refresh', 30);
        
        expect(tokenManager.shouldRefreshToken()).toBe(true);
    });
    
    test('should not refresh when plenty of time remaining', () => {
        // Token valid for 10 minutes
        tokenManager.setTokens('token', 'refresh', 600);
        
        expect(tokenManager.shouldRefreshToken()).toBe(false);
    });
});
```

---

## Monitoring Examples

### Backend Monitoring

```java
@Component
@Slf4j
public class TokenRefreshMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void onTokenRefresh(TokenRefreshEvent event) {
        // Count refresh events
        meterRegistry.counter("auth.token.refresh.count").increment();
        
        // Record time to refresh
        meterRegistry.timer("auth.token.refresh.duration")
                .record(event.getDuration(), TimeUnit.MILLISECONDS);
        
        // Log for debugging
        log.info("Token refreshed for user: {}, duration: {}ms", 
                event.getUserId(), event.getDuration());
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkRefreshRate() {
        double refreshRate = meterRegistry.counter("auth.token.refresh.count").count();
        
        if (refreshRate > 1000) {
            log.warn("High token refresh rate detected: {} per minute", refreshRate);
        }
    }
}
```

### Frontend Monitoring with Analytics

```typescript
import { tokenManager } from '../services/tokenManager';
import analytics from '../lib/analytics';

// Track token refresh events
const originalRefresh = tokenManager.refreshTokenIfNeeded.bind(tokenManager);

tokenManager.refreshTokenIfNeeded = async function() {
    const startTime = Date.now();
    
    try {
        const result = await originalRefresh();
        
        // Track successful refresh
        analytics.track('token_refresh_success', {
            duration: Date.now() - startTime,
            timestamp: new Date().toISOString()
        });
        
        return result;
    } catch (error) {
        // Track failed refresh
        analytics.track('token_refresh_failure', {
            error: error.message,
            timestamp: new Date().toISOString()
        });
        
        throw error;
    }
};
```

---

## Debugging Tips

### Enable Debug Logging

**Backend** (`application.yml`):
```yaml
logging:
  level:
    com.carbonmarketplace.userservice.security: DEBUG
    com.carbonmarketplace.userservice.service.AuthService: DEBUG
```

**Frontend** (Browser Console):
```javascript
// See all token manager logs
// They are prefixed with [TokenManager] and [API]

// Check current token status
console.log(tokenManager.getTokenStatus());

// Force a refresh
await tokenManager.refreshTokenIfNeeded();
```

### Common Issues and Solutions

1. **Token not refreshing**
   - Check browser console for errors
   - Verify API endpoint is accessible
   - Ensure refresh token is valid

2. **Multiple refresh requests**
   - Should be prevented by singleton pattern
   - Check network tab for duplicate requests

3. **Infinite redirect loop**
   - Check that auth endpoints are excluded from interceptor
   - Verify token validation logic

---

**Version**: 1.0  
**Last Updated**: October 26, 2025

