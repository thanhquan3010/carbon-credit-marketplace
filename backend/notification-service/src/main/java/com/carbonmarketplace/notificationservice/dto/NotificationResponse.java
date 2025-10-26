package com.carbonmarketplace.notificationservice.dto;

import com.carbonmarketplace.notificationservice.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private UUID notificationId;
    
    private UUID userId;
    
    private Notification.NotificationType type;
    
    private String title;
    
    private String message;
    
    private String actionUrl;
    
    private String actionLabel;
    
    private Notification.NotificationChannel channel;
    
    private Boolean isRead;
    
    private LocalDateTime readAt;
    
    private LocalDateTime sentAt;
    
    private Notification.DeliveryStatus deliveryStatus;
    
    private String deliveryError;
    
    private Map<String, String> data;
    
    private LocalDateTime createdAt;
}
