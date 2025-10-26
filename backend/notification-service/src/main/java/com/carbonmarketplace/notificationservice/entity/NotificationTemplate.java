package com.carbonmarketplace.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "template_id")
    private UUID templateId;
    
    @Column(name = "template_code", nullable = false, unique = true)
    private String templateCode;
    
    @Column(name = "template_name", nullable = false)
    private String templateName;
    
    @Column(name = "description")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private Notification.NotificationType notificationType;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "template_channels", joinColumns = @JoinColumn(name = "template_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    @Builder.Default
    private Set<Notification.NotificationChannel> supportedChannels = new HashSet<>();
    
    // Email Template Fields
    @Column(name = "email_subject")
    private String emailSubject;
    
    @Column(name = "email_body_html", columnDefinition = "TEXT")
    private String emailBodyHtml;
    
    @Column(name = "email_body_text", columnDefinition = "TEXT")
    private String emailBodyText;
    
    @Column(name = "email_from_name")
    private String emailFromName;
    
    @Column(name = "email_from_address")
    private String emailFromAddress;
    
    // SMS Template Fields
    @Column(name = "sms_body", length = 500)
    private String smsBody;
    
    // Push Notification Template Fields
    @Column(name = "push_title", length = 100)
    private String pushTitle;
    
    @Column(name = "push_body", length = 255)
    private String pushBody;
    
    @Column(name = "push_icon_url")
    private String pushIconUrl;
    
    @Column(name = "push_image_url")
    private String pushImageUrl;
    
    @Column(name = "push_action_url")
    private String pushActionUrl;
    
    // In-App Template Fields
    @Column(name = "in_app_title")
    private String inAppTitle;
    
    @Column(name = "in_app_body", columnDefinition = "TEXT")
    private String inAppBody;
    
    @Column(name = "in_app_icon")
    private String inAppIcon;
    
    @Column(name = "in_app_action_url")
    private String inAppActionUrl;
    
    @Column(name = "in_app_action_label")
    private String inAppActionLabel;
    
    // Template Configuration
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;
    
    @Column(name = "language_code")
    @Builder.Default
    private String languageCode = "en";
    
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;
    
    // Template Variables - JSON array of expected variables
    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables; // JSON array like ["username", "amount", "date"]
    
    // Template Engine
    @Enumerated(EnumType.STRING)
    @Column(name = "template_engine")
    @Builder.Default
    private TemplateEngine templateEngine = TemplateEngine.FREEMARKER;
    
    // Retry Configuration
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;
    
    @Column(name = "retry_interval_minutes")
    @Builder.Default
    private Integer retryIntervalMinutes = 5;
    
    // Rate Limiting
    @Column(name = "rate_limit_per_user_hour")
    private Integer rateLimitPerUserHour;
    
    @Column(name = "rate_limit_per_user_day")
    private Integer rateLimitPerUserDay;
    
    // Metadata
    @Column(name = "created_by")
    private String createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum TemplateEngine {
        FREEMARKER,
        THYMELEAF,
        VELOCITY,
        PLAIN
    }
}
