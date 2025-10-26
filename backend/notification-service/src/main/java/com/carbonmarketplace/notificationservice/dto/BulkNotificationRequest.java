package com.carbonmarketplace.notificationservice.dto;

import com.carbonmarketplace.notificationservice.entity.Notification;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {
    
    @NotEmpty(message = "User IDs list cannot be empty")
    private List<UUID> userIds;
    
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
    
    private Boolean immediate;
    
    private String scheduledFor;
    
    private String idempotencyKey;
    
    private Boolean useUserPreferences;
}
