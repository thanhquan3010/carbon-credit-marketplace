package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.dto.NotificationMessage;
import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationService {
    
    private final NotificationService notificationService;
    private final TemplateService templateService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @RabbitListener(queues = "${rabbitmq.queue.in-app}")
    public void processInAppNotification(NotificationMessage message) {
        log.info("Processing in-app notification: {} for user: {}", 
                message.getNotificationId(), message.getUserId());
        
        try {
            // Get notification content
            String title = message.getTitle();
            String body = message.getMessage();
            String actionUrl = message.getActionUrl();
            String actionLabel = message.getActionLabel();
            String icon = null;
            
            if (message.getTemplateCode() != null) {
                NotificationTemplate template = templateService.getTemplate(
                        message.getTemplateCode(),
                        "en", // TODO: Get user's language preference
                        Notification.NotificationChannel.IN_APP);
                
                if (template != null) {
                    Map<String, Object> variables = message.getTemplateVariables();
                    if (template.getInAppTitle() != null) {
                        title = templateService.processTemplate(template.getInAppTitle(), variables);
                    }
                    if (template.getInAppBody() != null) {
                        body = templateService.processTemplate(template.getInAppBody(), variables);
                    }
                    if (template.getInAppActionUrl() != null) {
                        actionUrl = template.getInAppActionUrl();
                    }
                    if (template.getInAppActionLabel() != null) {
                        actionLabel = template.getInAppActionLabel();
                    }
                    icon = template.getInAppIcon();
                }
            }
            
            // Create WebSocket payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", message.getNotificationId());
            payload.put("type", message.getType());
            payload.put("title", title);
            payload.put("message", body);
            payload.put("actionUrl", actionUrl);
            payload.put("actionLabel", actionLabel);
            payload.put("icon", icon);
            payload.put("timestamp", message.getTimestamp());
            payload.put("data", message.getData());
            
            // Send via WebSocket to user's private channel
            String userDestination = "/queue/notifications/" + message.getUserId();
            messagingTemplate.convertAndSend(userDestination, payload);
            
            // Update delivery status
            notificationService.updateDeliveryStatus(
                    message.getNotificationId(),
                    Notification.DeliveryStatus.DELIVERED,
                    null);
            
            log.info("In-app notification delivered to user: {}", message.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to deliver in-app notification: {}", message.getNotificationId(), e);
            handleFailure(message, e);
        }
    }
    
    /**
     * Send real-time notification to all users watching a specific topic
     */
    public void broadcastToTopic(String topic, Map<String, Object> payload) {
        try {
            String topicDestination = "/topic/" + topic;
            messagingTemplate.convertAndSend(topicDestination, payload);
            log.info("Broadcast notification sent to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to broadcast to topic: {}", topic, e);
        }
    }
    
    /**
     * Send real-time notification for marketplace events
     */
    public void notifyMarketplaceEvent(String eventType, Map<String, Object> eventData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("data", eventData);
        payload.put("timestamp", System.currentTimeMillis());
        
        broadcastToTopic("marketplace." + eventType, payload);
    }
    
    /**
     * Send real-time notification for auction updates
     */
    public void notifyAuctionUpdate(String auctionId, Map<String, Object> updateData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", auctionId);
        payload.put("update", updateData);
        payload.put("timestamp", System.currentTimeMillis());
        
        broadcastToTopic("auction." + auctionId, payload);
    }
    
    private void handleFailure(NotificationMessage message, Exception e) {
        // In-app notifications typically don't need retries since they're real-time
        // Just mark as failed and store for later retrieval
        notificationService.updateDeliveryStatus(
                message.getNotificationId(),
                Notification.DeliveryStatus.FAILED,
                e.getMessage());
    }
}
