package com.carbonmarketplace.notificationservice.dto;

import com.carbonmarketplace.notificationservice.entity.Notification;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Notification type is required")
    private Notification.NotificationType type;
    
    private String title;
    
    private String message;
    
    private String actionUrl;
    
    private String actionLabel;
    
    private Set<Notification.NotificationChannel> channels;
    
    private Map<String, String> data;
    
    private String templateCode;
    
    private Map<String, Object> templateVariables;
    
    private Integer priority;
    
    private String recipientEmail;
    
    private String recipientPhone;
    
    private String recipientDeviceToken;
    
    private Boolean immediate;
    
    private String scheduledFor; // ISO 8601 datetime string
    
    private String idempotencyKey; // To prevent duplicate notifications
}
