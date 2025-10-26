# JWT Token Refresh - Quick Reference Card

## 🚀 Quick Start

### Backend Setup
```java
// 1. Already enabled in UserServiceApplication.java
@EnableScheduling

// 2. Configuration in application.yml
spring.security.jwt.expiration: 900000  # 15 min
spring.security.jwt.refresh-expiration: 86400000  # 24 hours
```

### Frontend Setup
```typescript
// Import in your app entry point
import { tokenManager } from './services/tokenManager';

// Token manager automatically starts monitoring!
```

---

## 📋 Common Tasks

### Check Token Status
```typescript
const status = tokenManager.getTokenStatus();
console.log(status);
// { hasToken: true, expiresIn: 450, shouldRefresh: false, isExpired: false }
```

### Manual Token Refresh
```typescript
await tokenManager.refreshTokenIfNeeded();
```

### Show Token Status in UI
```tsx
import { TokenStatusIndicator } from './components/common/TokenStatusIndicator';

<TokenStatusIndicator showDetails={true} />
```

### Use Token Status Hook
```tsx
const { expiresIn, shouldRefresh } = useTokenStatus();
```

---

## 🔑 Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/login` | POST | Login and get tokens |
| `/auth/refresh` | POST | Refresh access token |
| `/auth/logout` | POST | Revoke tokens |
| `/auth/token/status` | GET | Check token status |

---

## ⚡ How It Works

```
Login → Store Tokens → Monitor Every 30s → Auto-Refresh at 60s → Repeat
```

### Automatic Process
1. User logs in → Tokens stored
2. Background monitor checks every 30 seconds
3. When < 60 seconds remaining → Auto-refresh
4. New tokens stored, old refresh token revoked
5. Process continues until logout

---

## 🎯 Key Features

| Feature | Description |
|---------|-------------|
| **Proactive Refresh** | Refreshes before expiry (< 60s) |
| **Token Rotation** | New tokens on each refresh |
| **Background Monitor** | Checks every 30 seconds |
| **Request Interceptor** | Auto-refresh before API calls |
| **Response Interceptor** | Fallback on 401 errors |
| **Scheduled Cleanup** | Daily at 2 AM |

---

## 🔐 Security

✅ Access tokens: 15 minutes  
✅ Refresh tokens: 24 hours  
✅ Old tokens immediately revoked  
✅ Singleton prevents duplicate refreshes  
✅ Audit trail (7-day retention)  

---

## 📊 Status Indicators

| Color | Meaning |
|-------|---------|
| 🟢 Green | > 5 minutes remaining |
| 🟠 Orange | < 5 minutes remaining |
| 🟡 Yellow | Refreshing (< 60s) |
| 🔴 Red | Expired |

---

## 🐛 Debug

### Backend Logs
```bash
# Enable debug logging
logging.level.com.carbonmarketplace.userservice.security: DEBUG
```

### Frontend Console
```javascript
// Check token status
console.log(tokenManager.getTokenStatus());

// Watch for logs
// [TokenManager] Token expires in 45 seconds
// [TokenManager] Auto-refreshing token
// [API] Token refresh failed
```

### Check Endpoint
```bash
curl -H "Authorization: Bearer {token}" \
     http://localhost:8081/auth/token/status
```

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| Token not refreshing | Check console logs, verify refresh endpoint |
| Multiple refreshes | Should not happen (singleton pattern) |
| Unexpected logout | Check token expiration config |
| 401 errors | Verify refresh token is valid |

---

## 📝 Code Snippets

### Login with Token Storage
```typescript
const response = await authService.login(email, password);
// Tokens automatically stored and monitoring started!
```

### Protected Route
```tsx
<ProtectedRoute>
  <Dashboard />
</ProtectedRoute>
```

### API Call (Auto-refresh)
```typescript
// Just make the call - refresh happens automatically
const data = await apiClient.get('/api/dashboard');
```

### Show Token Timer
```tsx
const { expiresInMinutes, expiresInSeconds } = useTokenStatus();
return <div>Token expires: {expiresInMinutes}m {expiresInSeconds}s</div>;
```

---

## 📦 Files Created

### Backend
- `JwtTokenProvider.java` (enhanced)
- `TokenExpirationResponse.java` (new DTO)
- `AuthController.java` (new endpoint)
- `AuthService.java` (new method)
- `RefreshTokenCleanupService.java` (new service)
- `UserServiceApplication.java` (@EnableScheduling)

### Frontend
- `tokenManager.ts` (new service) ⭐
- `api.ts` (enhanced interceptors)
- `authService.ts` (token manager integration)
- `useTokenStatus.ts` (new hook)
- `TokenStatusIndicator.tsx` (new component)

### Documentation
- `JWT_TOKEN_REFRESH_IMPLEMENTATION.md`
- `JWT_TOKEN_REFRESH_USAGE_EXAMPLES.md`
- `JWT_TOKEN_REFRESH_SUMMARY.md`
- `JWT_TOKEN_REFRESH_QUICK_REFERENCE.md` (this file)

---

## ✅ Deployment Checklist

- [ ] Set JWT_SECRET in production
- [ ] Test token refresh flow
- [ ] Verify cleanup job runs
- [ ] Check monitoring/alerts
- [ ] Validate CORS settings
- [ ] Test long operations

---

## 🎓 Best Practices

1. ✅ **Never** store tokens in cookies without HttpOnly
2. ✅ **Always** use HTTPS in production
3. ✅ **Monitor** token refresh rates
4. ✅ **Set** reasonable expiration times
5. ✅ **Revoke** all tokens on password change
6. ✅ **Log** suspicious refresh patterns

---

## 📞 Quick Help

**Token expired during operation?**
→ Should auto-refresh. Check console logs.

**Need to force refresh?**
```typescript
await tokenManager.refreshTokenIfNeeded();
```

**Want to see token status?**
```tsx
<TokenStatusIndicator showDetails />
```

**Admin cleanup needed?**
```java
cleanupService.performManualCleanup();
```

---

## 🔗 Related Documentation

- Full implementation details → `JWT_TOKEN_REFRESH_IMPLEMENTATION.md`
- Code examples → `JWT_TOKEN_REFRESH_USAGE_EXAMPLES.md`
- Complete summary → `JWT_TOKEN_REFRESH_SUMMARY.md`
- Original issue → `CURSOR_INSTRUCTIONS.md` (Issue #1)

---

**Version**: 1.0  
**Quick Reference**: Print this for easy access  
**Status**: ✅ Production Ready

---

## 💡 Remember

> The token manager handles everything automatically!  
> Just use authService.login() and make API calls normally.  
> Token refresh happens transparently in the background. 🎉


