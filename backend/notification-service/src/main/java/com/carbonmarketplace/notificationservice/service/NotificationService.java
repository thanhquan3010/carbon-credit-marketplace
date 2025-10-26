package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.dto.BulkNotificationRequest;
import com.carbonmarketplace.notificationservice.dto.NotificationMessage;
import com.carbonmarketplace.notificationservice.dto.NotificationRequest;
import com.carbonmarketplace.notificationservice.dto.NotificationResponse;
import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationPreference;
import com.carbonmarketplace.notificationservice.repository.NotificationPreferenceRepository;
import com.carbonmarketplace.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final TemplateService templateService;
    private final RateLimitService rateLimitService;
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;
    
    @Value("${rabbitmq.routing.email}")
    private String emailRoutingKey;
    
    @Value("${rabbitmq.routing.sms}")
    private String smsRoutingKey;
    
    @Value("${rabbitmq.routing.push}")
    private String pushRoutingKey;
    
    @Value("${rabbitmq.routing.in-app}")
    private String inAppRoutingKey;
    
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Processing notification request for user: {} of type: {}", 
                request.getUserId(), request.getType());
        
        // Check for duplicate notification using idempotency key
        if (request.getIdempotencyKey() != null) {
            Optional<Notification> existing = findExistingNotification(
                    request.getUserId(), request.getType(), request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate notification detected with idempotency key: {}", 
                        request.getIdempotencyKey());
                return toResponse(existing.get());
            }
        }
        
        // Get user preferences
        NotificationPreference preferences = getOrCreateUserPreferences(request.getUserId());
        
        // Determine channels to use
        Set<Notification.NotificationChannel> channels = determineChannels(request, preferences);
        
        if (channels.isEmpty()) {
            log.warn("No channels enabled for user: {}", request.getUserId());
            return null;
        }
        
        List<Notification> notifications = new ArrayList<>();
        
        for (Notification.NotificationChannel channel : channels) {
            // Check rate limits
            if (!rateLimitService.checkRateLimit(request.getUserId(), channel)) {
                log.warn("Rate limit exceeded for user: {} on channel: {}", 
                        request.getUserId(), channel);
                continue;
            }
            
            // Create notification entity
            Notification notification = createNotification(request, channel);
            notification = notificationRepository.save(notification);
            notifications.add(notification);
            
            // Queue for delivery
            queueNotification(notification, request);
        }
        
        return notifications.isEmpty() ? null : toResponse(notifications.get(0));
    }
    
    @Async
    @Transactional
    public void sendBulkNotifications(BulkNotificationRequest request) {
        log.info("Processing bulk notification request for {} users", request.getUserIds().size());
        
        for (UUID userId : request.getUserIds()) {
            try {
                NotificationRequest singleRequest = NotificationRequest.builder()
                        .userId(userId)
                        .type(request.getType())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .actionUrl(request.getActionUrl())
                        .actionLabel(request.getActionLabel())
                        .channels(request.getChannels())
                        .data(request.getData())
                        .templateCode(request.getTemplateCode())
                        .templateVariables(request.getTemplateVariables())
                        .priority(request.getPriority())
                        .immediate(request.getImmediate())
                        .scheduledFor(request.getScheduledFor())
                        .idempotencyKey(request.getIdempotencyKey() + "-" + userId)
                        .build();
                
                sendNotification(singleRequest);
            } catch (Exception e) {
                log.error("Failed to send notification to user: {}", userId, e);
            }
        }
    }
    
    private Notification createNotification(NotificationRequest request, 
                                           Notification.NotificationChannel channel) {
        return Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .channel(channel)
                .data(request.getData())
                .templateId(request.getTemplateCode() != null ? 
                        templateService.getTemplateId(request.getTemplateCode()) : null)
                .recipientEmail(request.getRecipientEmail())
                .recipientPhone(request.getRecipientPhone())
                .recipientDeviceToken(request.getRecipientDeviceToken())
                .priority(request.getPriority() != null ? request.getPriority() : 5)
                .deliveryStatus(Notification.DeliveryStatus.PENDING)
                .build();
    }
    
    private void queueNotification(Notification notification, NotificationRequest request) {
        NotificationMessage message = NotificationMessage.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .data(notification.getData())
                .templateVariables(request.getTemplateVariables())
                .templateCode(request.getTemplateCode())
                .templateId(notification.getTemplateId())
                .recipientEmail(notification.getRecipientEmail())
                .recipientPhone(notification.getRecipientPhone())
                .recipientDeviceToken(notification.getRecipientDeviceToken())
                .priority(notification.getPriority())
                .retryCount(notification.getRetryCount())
                .maxRetries(notification.getMaxRetries())
                .timestamp(LocalDateTime.now())
                .build();
        
        String routingKey = getRoutingKey(notification.getChannel());
        
        try {
            rabbitTemplate.convertAndSend(notificationExchange, routingKey, message);
            
            notification.setDeliveryStatus(Notification.DeliveryStatus.QUEUED);
            notificationRepository.save(notification);
            
            log.info("Notification {} queued for delivery via {}", 
                    notification.getNotificationId(), notification.getChannel());
        } catch (Exception e) {
            log.error("Failed to queue notification {}", notification.getNotificationId(), e);
            notification.setDeliveryStatus(Notification.DeliveryStatus.FAILED);
            notification.setDeliveryError(e.getMessage());
            notificationRepository.save(notification);
        }
    }
    
    private String getRoutingKey(Notification.NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailRoutingKey;
            case SMS -> smsRoutingKey;
            case PUSH -> pushRoutingKey;
            case IN_APP -> inAppRoutingKey;
        };
    }
    
    private Set<Notification.NotificationChannel> determineChannels(
            NotificationRequest request, NotificationPreference preferences) {
        
        if (request.getChannels() != null && !request.getChannels().isEmpty()) {
            // Filter requested channels based on user preferences
            return request.getChannels().stream()
                    .filter(channel -> isChannelEnabled(channel, preferences))
                    .collect(Collectors.toSet());
        }
        
        // Auto-determine channels based on notification type and preferences
        Set<Notification.NotificationChannel> channels = new HashSet<>();
        
        if (preferences.getInAppEnabled()) {
            channels.add(Notification.NotificationChannel.IN_APP);
        }
        
        // Add email for important notifications
        if (preferences.getEmailEnabled() && isImportantType(request.getType())) {
            channels.add(Notification.NotificationChannel.EMAIL);
        }
        
        // Add SMS for critical notifications
        if (preferences.getSmsEnabled() && isCriticalType(request.getType())) {
            channels.add(Notification.NotificationChannel.SMS);
        }
        
        // Add push if enabled
        if (preferences.getPushEnabled() && request.getRecipientDeviceToken() != null) {
            channels.add(Notification.NotificationChannel.PUSH);
        }
        
        return channels;
    }
    
    private boolean isChannelEnabled(Notification.NotificationChannel channel, 
                                    NotificationPreference preferences) {
        return switch (channel) {
            case EMAIL -> preferences.getEmailEnabled();
            case SMS -> preferences.getSmsEnabled();
            case PUSH -> preferences.getPushEnabled();
            case IN_APP -> preferences.getInAppEnabled();
        };
    }
    
    private boolean isImportantType(Notification.NotificationType type) {
        return switch (type) {
            case KYC_APPROVED, KYC_REJECTED, VERIFICATION_APPROVED, 
                 VERIFICATION_REJECTED, PAYMENT_RECEIVED, PAYMENT_FAILED -> true;
            default -> false;
        };
    }
    
    private boolean isCriticalType(Notification.NotificationType type) {
        return switch (type) {
            case TWO_FACTOR_AUTH, PASSWORD_RESET, PAYMENT_FAILED -> true;
            default -> false;
        };
    }
    
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::toResponse);
    }
    
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    @Transactional
    public boolean markAsRead(UUID notificationId, UUID userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        return updated > 0;
    }
    
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }
    
    @Transactional
    public void updateDeliveryStatus(UUID notificationId, 
                                    Notification.DeliveryStatus status, 
                                    String error) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setDeliveryStatus(status);
            notification.setDeliveryError(error);
            if (status == Notification.DeliveryStatus.SENT || 
                status == Notification.DeliveryStatus.DELIVERED) {
                notification.setSentAt(LocalDateTime.now());
            }
            notificationRepository.save(notification);
        });
    }
    
    private NotificationPreference getOrCreateUserPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference newPreference = NotificationPreference.builder()
                            .userId(userId)
                            .build();
                    return preferenceRepository.save(newPreference);
                });
    }
    
    private Optional<Notification> findExistingNotification(UUID userId, 
                                                           Notification.NotificationType type,
                                                           String idempotencyKey) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        return notificationRepository.findByUserIdAndTypeAndDataAndCreatedAtAfter(
                userId, type, idempotencyKey, cutoff);
    }
    
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .channel(notification.getChannel())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .sentAt(notification.getSentAt())
                .deliveryStatus(notification.getDeliveryStatus())
                .deliveryError(notification.getDeliveryError())
                .data(notification.getData())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
