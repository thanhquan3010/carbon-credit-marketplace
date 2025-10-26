package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.dto.NotificationMessage;
import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {
    
    private final NotificationRepository notificationRepository;
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
    
    @Value("${notification.retry.enabled:true}")
    private boolean retryEnabled;
    
    @Value("${notification.retry.max-attempts:3}")
    private int defaultMaxRetries;
    
    @Value("${notification.retry.base-delay-minutes:5}")
    private int baseDelayMinutes;
    
    /**
     * Scheduled job to retry failed notifications
     */
    @Scheduled(fixedDelayString = "${notification.retry.check-interval:60000}") // Check every minute
    @Transactional
    public void retryFailedNotifications() {
        if (!retryEnabled) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        List<Notification> retryableNotifications = notificationRepository.findRetryableNotifications(
                Notification.DeliveryStatus.RETRY, now);
        
        if (!retryableNotifications.isEmpty()) {
            log.info("Found {} notifications to retry", retryableNotifications.size());
            
            for (Notification notification : retryableNotifications) {
                retryNotification(notification);
            }
        }
    }
    
    /**
     * Retry a single notification
     */
    public void retryNotification(Notification notification) {
        try {
            log.info("Retrying notification {} (attempt {} of {})",
                    notification.getNotificationId(),
                    notification.getRetryCount() + 1,
                    notification.getMaxRetries());
            
            // Increment retry count
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setDeliveryStatus(Notification.DeliveryStatus.QUEUED);
            
            // Calculate next retry time using exponential backoff
            LocalDateTime nextRetry = calculateNextRetryTime(notification.getRetryCount());
            notification.setNextRetryAt(nextRetry);
            
            notificationRepository.save(notification);
            
            // Create message for retry
            NotificationMessage message = buildNotificationMessage(notification);
            message.setRetryCount(notification.getRetryCount());
            
            // Send to appropriate queue
            String routingKey = getRoutingKey(notification.getChannel());
            rabbitTemplate.convertAndSend(notificationExchange, routingKey, message);
            
            log.info("Notification {} requeued for retry", notification.getNotificationId());
            
        } catch (Exception e) {
            log.error("Failed to retry notification {}", notification.getNotificationId(), e);
            
            // If we can't even retry, mark as failed
            notification.setDeliveryStatus(Notification.DeliveryStatus.FAILED);
            notification.setDeliveryError("Failed to retry: " + e.getMessage());
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Calculate next retry time using exponential backoff
     */
    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // Exponential backoff: delay = baseDelay * 2^(retryCount - 1)
        int delayMinutes = baseDelayMinutes * (int) Math.pow(2, retryCount - 1);
        
        // Cap at 24 hours
        delayMinutes = Math.min(delayMinutes, 1440);
        
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }
    
    /**
     * Mark expired notifications as failed
     */
    @Scheduled(fixedDelayString = "${notification.expiry.check-interval:3600000}") // Check every hour
    @Transactional
    public void markExpiredNotifications() {
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(24);
        List<Notification> expiredNotifications = notificationRepository.findExpiredNotifications(expirationTime);
        
        if (!expiredNotifications.isEmpty()) {
            log.info("Found {} expired notifications", expiredNotifications.size());
            
            for (Notification notification : expiredNotifications) {
                notification.setDeliveryStatus(Notification.DeliveryStatus.EXPIRED);
                notification.setDeliveryError("Notification expired after 24 hours");
                notificationRepository.save(notification);
            }
        }
    }
    
    /**
     * Clean up old notifications
     */
    @Scheduled(cron = "${notification.cleanup.cron:0 0 2 * * ?}") // Daily at 2 AM
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deleted = notificationRepository.deleteOldReadNotifications(cutoffDate);
        
        if (deleted > 0) {
            log.info("Deleted {} old read notifications", deleted);
        }
    }
    
    private NotificationMessage buildNotificationMessage(Notification notification) {
        return NotificationMessage.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .data(notification.getData())
                .templateId(notification.getTemplateId())
                .recipientEmail(notification.getRecipientEmail())
                .recipientPhone(notification.getRecipientPhone())
                .recipientDeviceToken(notification.getRecipientDeviceToken())
                .priority(notification.getPriority())
                .retryCount(notification.getRetryCount())
                .maxRetries(notification.getMaxRetries())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private String getRoutingKey(Notification.NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailRoutingKey;
            case SMS -> smsRoutingKey;
            case PUSH -> pushRoutingKey;
            case IN_APP -> inAppRoutingKey;
        };
    }
    
    /**
     * Get retry statistics
     */
    public RetryStats getRetryStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayAgo = now.minusDays(1);
        
        List<Object[]> stats = notificationRepository.getDeliveryStatsByChannel(dayAgo, now);
        
        RetryStats retryStats = new RetryStats();
        for (Object[] stat : stats) {
            Notification.NotificationChannel channel = (Notification.NotificationChannel) stat[0];
            Notification.DeliveryStatus status = (Notification.DeliveryStatus) stat[1];
            Long count = (Long) stat[2];
            
            retryStats.addStat(channel, status, count);
        }
        
        return retryStats;
    }
    
    public static class RetryStats {
        private final java.util.Map<Notification.NotificationChannel, java.util.Map<Notification.DeliveryStatus, Long>> stats = new java.util.HashMap<>();
        
        public void addStat(Notification.NotificationChannel channel, Notification.DeliveryStatus status, Long count) {
            stats.computeIfAbsent(channel, k -> new java.util.HashMap<>()).put(status, count);
        }
        
        public java.util.Map<Notification.NotificationChannel, java.util.Map<Notification.DeliveryStatus, Long>> getStats() {
            return stats;
        }
        
        public long getTotalRetries() {
            return stats.values().stream()
                    .flatMap(m -> m.entrySet().stream())
                    .filter(e -> e.getKey() == Notification.DeliveryStatus.RETRY)
                    .mapToLong(java.util.Map.Entry::getValue)
                    .sum();
        }
        
        public long getTotalFailed() {
            return stats.values().stream()
                    .flatMap(m -> m.entrySet().stream())
                    .filter(e -> e.getKey() == Notification.DeliveryStatus.FAILED)
                    .mapToLong(java.util.Map.Entry::getValue)
                    .sum();
        }
    }
}
