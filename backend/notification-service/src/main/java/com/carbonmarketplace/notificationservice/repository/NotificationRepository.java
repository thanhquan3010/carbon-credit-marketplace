package com.carbonmarketplace.notificationservice.repository;

import com.carbonmarketplace.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    // Find by user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(
            UUID userId, Notification.NotificationChannel channel, Pageable pageable);
    
    // Unread notifications
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);
    
    Long countByUserIdAndIsReadFalse(UUID userId);
    
    // Find by status
    List<Notification> findByDeliveryStatusAndNextRetryAtBefore(
            Notification.DeliveryStatus status, LocalDateTime dateTime);
    
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = :status " +
           "AND n.retryCount < n.maxRetries AND n.nextRetryAt <= :now")
    List<Notification> findRetryableNotifications(
            @Param("status") Notification.DeliveryStatus status,
            @Param("now") LocalDateTime now);
    
    // Mark as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.notificationId = :notificationId AND n.userId = :userId")
    int markAsRead(@Param("notificationId") UUID notificationId,
                   @Param("userId") UUID userId,
                   @Param("readAt") LocalDateTime readAt);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
    
    // Delete old notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate " +
           "AND n.isRead = true")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Find by idempotency key
    Optional<Notification> findByUserIdAndTypeAndDataAndCreatedAtAfter(
            UUID userId, 
            Notification.NotificationType type,
            String idempotencyKey,
            LocalDateTime after);
    
    // Analytics queries
    @Query("SELECT n.type, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.type")
    List<Object[]> getNotificationStatsByType(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    @Query("SELECT n.channel, n.deliveryStatus, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt BETWEEN :start AND :end " +
           "GROUP BY n.channel, n.deliveryStatus")
    List<Object[]> getDeliveryStatsByChannel(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    // Find expired notifications
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'PENDING' " +
           "AND n.createdAt < :expirationTime")
    List<Notification> findExpiredNotifications(@Param("expirationTime") LocalDateTime expirationTime);
    
    // User notification history
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND n.type = :type AND n.createdAt > :since")
    List<Notification> findRecentNotificationsByUserAndType(
            @Param("userId") UUID userId,
            @Param("type") Notification.NotificationType type,
            @Param("since") LocalDateTime since);
    
    // Count for rate limiting
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId " +
           "AND n.channel = :channel AND n.sentAt > :since")
    Long countRecentNotificationsByUserAndChannel(
            @Param("userId") UUID userId,
            @Param("channel") Notification.NotificationChannel channel,
            @Param("since") LocalDateTime since);
}
