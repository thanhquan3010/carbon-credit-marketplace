package com.carbonmarketplace.notificationservice.dto;

import com.carbonmarketplace.notificationservice.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    
    private UUID notificationId;
    
    private UUID userId;
    
    private Notification.NotificationType type;
    
    private Notification.NotificationChannel channel;
    
    private String title;
    
    private String message;
    
    private String actionUrl;
    
    private String actionLabel;
    
    private Map<String, String> data;
    
    private Map<String, Object> templateVariables;
    
    private String templateCode;
    
    private UUID templateId;
    
    private String recipientEmail;
    
    private String recipientPhone;
    
    private String recipientDeviceToken;
    
    private Integer priority;
    
    private Integer retryCount;
    
    private Integer maxRetries;
    
    private LocalDateTime scheduledFor;
    
    private String idempotencyKey;
    
    private LocalDateTime timestamp;
}
