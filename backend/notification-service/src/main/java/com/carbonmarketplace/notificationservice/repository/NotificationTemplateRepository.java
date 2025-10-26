package com.carbonmarketplace.notificationservice.repository;

import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    
    Optional<NotificationTemplate> findByTemplateCodeAndIsActiveTrue(String templateCode);
    
    Optional<NotificationTemplate> findByTemplateCodeAndLanguageCodeAndIsActiveTrue(
            String templateCode, String languageCode);
    
    List<NotificationTemplate> findByNotificationTypeAndIsActiveTrue(
            Notification.NotificationType notificationType);
    
    @Query("SELECT t FROM NotificationTemplate t WHERE t.notificationType = :type " +
           "AND t.languageCode = :language AND t.isActive = true " +
           "AND :channel MEMBER OF t.supportedChannels")
    Optional<NotificationTemplate> findByTypeAndLanguageAndChannel(
            @Param("type") Notification.NotificationType type,
            @Param("language") String language,
            @Param("channel") Notification.NotificationChannel channel);
    
    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
           "AND :channel MEMBER OF t.supportedChannels")
    List<NotificationTemplate> findByChannel(@Param("channel") Notification.NotificationChannel channel);
    
    List<NotificationTemplate> findByIsActiveTrue();
    
    @Query("SELECT DISTINCT t.languageCode FROM NotificationTemplate t WHERE t.isActive = true")
    List<String> findDistinctLanguageCodes();
    
    @Query("SELECT t FROM NotificationTemplate t WHERE t.templateCode = :code " +
           "ORDER BY t.version DESC")
    List<NotificationTemplate> findTemplateVersionHistory(@Param("code") String templateCode);
    
    boolean existsByTemplateCodeAndIsActiveTrue(String templateCode);
}
