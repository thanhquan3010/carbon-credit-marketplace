package com.carbonmarketplace.notificationservice.controller;

import com.carbonmarketplace.notificationservice.dto.BulkNotificationRequest;
import com.carbonmarketplace.notificationservice.dto.NotificationRequest;
import com.carbonmarketplace.notificationservice.dto.NotificationResponse;
import com.carbonmarketplace.notificationservice.entity.NotificationPreference;
import com.carbonmarketplace.notificationservice.repository.NotificationPreferenceRepository;
import com.carbonmarketplace.notificationservice.service.NotificationService;
import com.carbonmarketplace.notificationservice.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final RateLimitService rateLimitService;
    
    @PostMapping("/send")
    @Operation(summary = "Send a notification", description = "Send a notification to a user through specified channels")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-bulk")
    @Operation(summary = "Send bulk notifications", description = "Send notifications to multiple users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> sendBulkNotifications(@Valid @RequestBody BulkNotificationRequest request) {
        notificationService.sendBulkNotifications(request);
        return ResponseEntity.ok(Map.of(
                "message", "Bulk notification processing initiated",
                "userCount", request.getUserIds().size()
        ));
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications", description = "Get paginated list of notifications for a user")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get unread notifications", description = "Get all unread notifications for a user")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@PathVariable UUID userId) {
        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications for a user")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable UUID userId) {
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
    
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    @PreAuthorize("hasRole('ADMIN') or @notificationService.isOwner(#notificationId, authentication.principal.userId)")
    public ResponseEntity<Map<String, Boolean>> markAsRead(
            @PathVariable UUID notificationId,
            @RequestParam UUID userId) {
        
        boolean success = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }
    
    @PatchMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read for a user")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable UUID userId) {
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedCount", updated
        ));
    }
    
    // Notification Preferences Endpoints
    
    @GetMapping("/preferences/{userId}")
    @Operation(summary = "Get notification preferences", description = "Get notification preferences for a user")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<NotificationPreference> getPreferences(@PathVariable UUID userId) {
        NotificationPreference preferences = preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder().userId(userId).build());
        return ResponseEntity.ok(preferences);
    }
    
    @PutMapping("/preferences/{userId}")
    @Operation(summary = "Update notification preferences", description = "Update notification preferences for a user")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @PathVariable UUID userId,
            @RequestBody NotificationPreference preferences) {
        
        NotificationPreference existing = preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder().userId(userId).build());
        
        // Update preferences
        existing.setEmailEnabled(preferences.getEmailEnabled());
        existing.setSmsEnabled(preferences.getSmsEnabled());
        existing.setPushEnabled(preferences.getPushEnabled());
        existing.setInAppEnabled(preferences.getInAppEnabled());
        existing.setMarketingEmails(preferences.getMarketingEmails());
        existing.setTransactionNotifications(preferences.getTransactionNotifications());
        existing.setTripSyncNotifications(preferences.getTripSyncNotifications());
        existing.setMarketplaceNotifications(preferences.getMarketplaceNotifications());
        existing.setVerificationNotifications(preferences.getVerificationNotifications());
        existing.setSystemNotifications(preferences.getSystemNotifications());
        existing.setQuietHoursEnabled(preferences.getQuietHoursEnabled());
        existing.setQuietHoursStart(preferences.getQuietHoursStart());
        existing.setQuietHoursEnd(preferences.getQuietHoursEnd());
        existing.setTimezone(preferences.getTimezone());
        existing.setDailyEmailLimit(preferences.getDailyEmailLimit());
        existing.setDailySmsLimit(preferences.getDailySmsLimit());
        existing.setDailyPushLimit(preferences.getDailyPushLimit());
        existing.setLanguagePreference(preferences.getLanguagePreference());
        existing.setDigestEnabled(preferences.getDigestEnabled());
        existing.setDigestFrequency(preferences.getDigestFrequency());
        
        NotificationPreference saved = preferenceRepository.save(existing);
        return ResponseEntity.ok(saved);
    }
    
    // Admin Endpoints
    
    @GetMapping("/stats/rate-limits/{userId}")
    @Operation(summary = "Get rate limit stats", description = "Get current rate limit statistics for a user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RateLimitService.RateLimitStats> getRateLimitStats(@PathVariable UUID userId) {
        RateLimitService.RateLimitStats stats = rateLimitService.getUserRateLimitStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/admin/reset-rate-limits/{userId}")
    @Operation(summary = "Reset rate limits", description = "Reset rate limits for a user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetRateLimits(@PathVariable UUID userId) {
        rateLimitService.resetRateLimits(userId);
        return ResponseEntity.ok(Map.of("message", "Rate limits reset successfully"));
    }
    
    // Health Check
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the notification service is running")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "notification-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}
