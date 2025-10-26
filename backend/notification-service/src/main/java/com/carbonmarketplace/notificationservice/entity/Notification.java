package com.carbonmarketplace.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 100)
    private NotificationType type;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "action_url")
    private String actionUrl;
    
    @Column(name = "action_label", length = 100)
    private String actionLabel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private NotificationChannel channel;
    
    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 50)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;
    
    @Column(name = "delivery_error", columnDefinition = "TEXT")
    private String deliveryError;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;
    
    @ElementCollection
    @CollectionTable(name = "notification_data", joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    @Builder.Default
    private Map<String, String> data = new HashMap<>();
    
    @Column(name = "template_id")
    private UUID templateId;
    
    @Column(name = "external_id")
    private String externalId; // External provider message ID (SendGrid, Twilio, etc.)
    
    @Column(name = "recipient_email")
    private String recipientEmail;
    
    @Column(name = "recipient_phone")
    private String recipientPhone;
    
    @Column(name = "recipient_device_token")
    private String recipientDeviceToken; // For push notifications
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum NotificationType {
        // User notifications
        USER_REGISTERED,
        KYC_SUBMITTED,
        KYC_APPROVED,
        KYC_REJECTED,
        PASSWORD_RESET,
        EMAIL_VERIFICATION,
        TWO_FACTOR_AUTH,
        
        // Vehicle notifications
        VEHICLE_ADDED,
        VEHICLE_VERIFIED,
        TRIP_SYNCED,
        TRIP_ANOMALY_DETECTED,
        
        // Carbon credit notifications
        CREDITS_EARNED,
        VERIFICATION_REQUESTED,
        VERIFICATION_APPROVED,
        VERIFICATION_REJECTED,
        
        // Marketplace notifications
        LISTING_CREATED,
        LISTING_SOLD,
        LISTING_EXPIRED,
        BID_PLACED,
        BID_WON,
        BID_OUTBID,
        AUCTION_ENDING_SOON,
        
        // Transaction notifications
        PAYMENT_RECEIVED,
        PAYMENT_SENT,
        PAYMENT_FAILED,
        WITHDRAWAL_COMPLETED,
        
        // System notifications
        SYSTEM_MAINTENANCE,
        FEATURE_UPDATE,
        POLICY_UPDATE,
        PROMOTIONAL
    }
    
    public enum NotificationChannel {
        EMAIL,
        SMS,
        IN_APP,
        PUSH
    }
    
    public enum DeliveryStatus {
        PENDING,
        QUEUED,
        SENDING,
        SENT,
        DELIVERED,
        FAILED,
        BOUNCED,
        RETRY,
        EXPIRED
    }
}
