package com.carbonmarketplace.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "preference_id")
    private UUID preferenceId;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    // Channel Preferences
    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = true;
    
    @Column(name = "sms_enabled")
    @Builder.Default
    private Boolean smsEnabled = true;
    
    @Column(name = "push_enabled")
    @Builder.Default
    private Boolean pushEnabled = true;
    
    @Column(name = "in_app_enabled")
    @Builder.Default
    private Boolean inAppEnabled = true;
    
    // Type-specific Preferences
    @Column(name = "marketing_emails")
    @Builder.Default
    private Boolean marketingEmails = true;
    
    @Column(name = "transaction_notifications")
    @Builder.Default
    private Boolean transactionNotifications = true;
    
    @Column(name = "trip_sync_notifications")
    @Builder.Default
    private Boolean tripSyncNotifications = true;
    
    @Column(name = "marketplace_notifications")
    @Builder.Default
    private Boolean marketplaceNotifications = true;
    
    @Column(name = "verification_notifications")
    @Builder.Default
    private Boolean verificationNotifications = true;
    
    @Column(name = "system_notifications")
    @Builder.Default
    private Boolean systemNotifications = true;
    
    // Quiet Hours (in user's timezone)
    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;
    
    @Column(name = "quiet_hours_start")
    private String quietHoursStart; // Format: "HH:mm"
    
    @Column(name = "quiet_hours_end")
    private String quietHoursEnd; // Format: "HH:mm"
    
    @Column(name = "timezone")
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";
    
    // Frequency Limits
    @Column(name = "daily_email_limit")
    @Builder.Default
    private Integer dailyEmailLimit = 10;
    
    @Column(name = "daily_sms_limit")
    @Builder.Default
    private Integer dailySmsLimit = 5;
    
    @Column(name = "daily_push_limit")
    @Builder.Default
    private Integer dailyPushLimit = 20;
    
    // Language Preference
    @Column(name = "language_preference")
    @Builder.Default
    private String languagePreference = "vi";
    
    // Digest Settings
    @Column(name = "digest_enabled")
    @Builder.Default
    private Boolean digestEnabled = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "digest_frequency")
    private DigestFrequency digestFrequency;
    
    @Column(name = "last_digest_sent_at")
    private LocalDateTime lastDigestSentAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum DigestFrequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}
