package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.dto.NotificationMessage;
import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationTemplate;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    
    private final NotificationService notificationService;
    private final TemplateService templateService;
    
    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;
    
    @Value("${firebase.config-file:firebase-config.json}")
    private String firebaseConfigFile;
    
    @Value("${firebase.project-id}")
    private String projectId;
    
    @PostConstruct
    public void init() {
        if (firebaseEnabled) {
            try {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ClassPathResource(firebaseConfigFile).getInputStream()))
                        .setProjectId(projectId)
                        .build();
                
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialized successfully");
                }
            } catch (IOException e) {
                log.error("Failed to initialize Firebase", e);
                firebaseEnabled = false;
            }
        }
    }
    
    @RabbitListener(queues = "${rabbitmq.queue.push}")
    public void processPushNotification(NotificationMessage message) {
        log.info("Processing push notification: {} for user: {}", 
                message.getNotificationId(), message.getUserId());
        
        try {
            // Validate device token
            if (message.getRecipientDeviceToken() == null || message.getRecipientDeviceToken().isEmpty()) {
                throw new IllegalArgumentException("Device token is required for push notifications");
            }
            
            // Get notification content
            String title = message.getTitle();
            String body = message.getMessage();
            String imageUrl = null;
            String actionUrl = message.getActionUrl();
            
            if (message.getTemplateCode() != null) {
                NotificationTemplate template = templateService.getTemplate(
                        message.getTemplateCode(),
                        "en", // TODO: Get user's language preference
                        Notification.NotificationChannel.PUSH);
                
                if (template != null) {
                    Map<String, Object> variables = message.getTemplateVariables();
                    if (template.getPushTitle() != null) {
                        title = templateService.processTemplate(template.getPushTitle(), variables);
                    }
                    if (template.getPushBody() != null) {
                        body = templateService.processTemplate(template.getPushBody(), variables);
                    }
                    imageUrl = template.getPushImageUrl();
                    if (template.getPushActionUrl() != null) {
                        actionUrl = template.getPushActionUrl();
                    }
                }
            }
            
            boolean sent = false;
            String externalId = null;
            
            if (firebaseEnabled) {
                externalId = sendViaFirebase(
                        message.getRecipientDeviceToken(), 
                        title, 
                        body, 
                        imageUrl,
                        actionUrl,
                        message.getData());
                sent = externalId != null;
            } else {
                // For testing/development, just log the notification
                log.info("Push (Mock): Token: {}, Title: {}, Body: {}", 
                        message.getRecipientDeviceToken(), title, body);
                sent = true;
            }
            
            if (sent) {
                notificationService.updateDeliveryStatus(
                        message.getNotificationId(),
                        Notification.DeliveryStatus.SENT,
                        null);
                log.info("Push notification sent successfully to token: {}", 
                        message.getRecipientDeviceToken());
            } else {
                throw new RuntimeException("Failed to send push notification");
            }
            
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", message.getNotificationId(), e);
            handleFailure(message, e);
        }
    }
    
    private String sendViaFirebase(String deviceToken, String title, String body, 
                                  String imageUrl, String actionUrl, Map<String, String> data) {
        try {
            // Build the notification
            com.google.firebase.messaging.Notification.Builder notificationBuilder = 
                    com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                notificationBuilder.setImage(imageUrl);
            }
            
            // Build the message
            Message.Builder messageBuilder = Message.builder()
                    .setNotification(notificationBuilder.build())
                    .setToken(deviceToken);
            
            // Add data payload
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            
            // Add action URL to data
            if (actionUrl != null && !actionUrl.isEmpty()) {
                messageBuilder.putData("action_url", actionUrl);
            }
            
            // Set Android specific configuration
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setClickAction("OPEN_ACTIVITY")
                            .build())
                    .build();
            messageBuilder.setAndroidConfig(androidConfig);
            
            // Set iOS specific configuration
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setAlert(ApsAlert.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .setBadge(1)
                            .setSound("default")
                            .build())
                    .build();
            messageBuilder.setApnsConfig(apnsConfig);
            
            // Send the message
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.debug("Firebase message sent successfully: {}", response);
            
            return response;
            
        } catch (FirebaseMessagingException e) {
            log.error("Firebase send error: {}", e.getMessage());
            
            // Handle specific error cases
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                // Device token is invalid - should remove from database
                log.warn("Device token is no longer valid: {}", deviceToken);
                // TODO: Remove invalid token from user's device tokens
            }
            
            return null;
        } catch (Exception e) {
            log.error("Unexpected error sending push notification", e);
            return null;
        }
    }
    
    private void handleFailure(NotificationMessage message, Exception e) {
        // Push notifications typically shouldn't be retried as aggressively
        if (message.getRetryCount() < Math.min(message.getMaxRetries(), 1)) {
            log.info("Requeueing push notification {} for retry (attempt {} of {})",
                    message.getNotificationId(),
                    message.getRetryCount() + 1,
                    message.getMaxRetries());
            
            notificationService.updateDeliveryStatus(
                    message.getNotificationId(),
                    Notification.DeliveryStatus.RETRY,
                    e.getMessage());
        } else {
            // Mark as failed
            notificationService.updateDeliveryStatus(
                    message.getNotificationId(),
                    Notification.DeliveryStatus.FAILED,
                    e.getMessage());
        }
    }
}
