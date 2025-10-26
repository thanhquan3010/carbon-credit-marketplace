package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationTemplate;
import com.carbonmarketplace.notificationservice.repository.NotificationTemplateRepository;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {
    
    private final NotificationTemplateRepository templateRepository;
    private final Configuration freemarkerConfig;
    private final SpringTemplateEngine thymeleafEngine;
    
    @Cacheable(value = "templates", key = "#templateCode + '-' + #language + '-' + #channel")
    public NotificationTemplate getTemplate(String templateCode, String language, 
                                           Notification.NotificationChannel channel) {
        return templateRepository.findByTypeAndLanguageAndChannel(
                getNotificationTypeFromCode(templateCode), language, channel)
                .orElse(null);
    }
    
    @Cacheable(value = "templates", key = "#templateCode")
    public NotificationTemplate getTemplateByCode(String templateCode) {
        return templateRepository.findByTemplateCodeAndIsActiveTrue(templateCode)
                .orElse(null);
    }
    
    public UUID getTemplateId(String templateCode) {
        NotificationTemplate template = getTemplateByCode(templateCode);
        return template != null ? template.getTemplateId() : null;
    }
    
    public String processTemplate(String templateContent, Map<String, Object> variables) {
        if (templateContent == null || templateContent.isEmpty()) {
            return templateContent;
        }
        
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        try {
            // Use FreeMarker for template processing
            Template template = new Template("template", 
                    new StringReader(templateContent), freemarkerConfig);
            StringWriter writer = new StringWriter();
            template.process(variables, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Failed to process template", e);
            // Fall back to simple string replacement
            return processSimpleTemplate(templateContent, variables);
        }
    }
    
    public String processThymeleafTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        return thymeleafEngine.process(templateName, context);
    }
    
    private String processSimpleTemplate(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    @Transactional
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        // Check if template with same code exists
        if (templateRepository.existsByTemplateCodeAndIsActiveTrue(template.getTemplateCode())) {
            throw new IllegalArgumentException("Template with code " + template.getTemplateCode() + " already exists");
        }
        
        template.setIsActive(true);
        template.setVersion(1);
        return templateRepository.save(template);
    }
    
    @Transactional
    public NotificationTemplate updateTemplate(UUID templateId, NotificationTemplate updates) {
        NotificationTemplate existing = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        
        // Create new version
        NotificationTemplate newVersion = NotificationTemplate.builder()
                .templateCode(existing.getTemplateCode())
                .templateName(updates.getTemplateName() != null ? updates.getTemplateName() : existing.getTemplateName())
                .description(updates.getDescription() != null ? updates.getDescription() : existing.getDescription())
                .notificationType(existing.getNotificationType())
                .supportedChannels(updates.getSupportedChannels() != null ? updates.getSupportedChannels() : existing.getSupportedChannels())
                .emailSubject(updates.getEmailSubject())
                .emailBodyHtml(updates.getEmailBodyHtml())
                .emailBodyText(updates.getEmailBodyText())
                .emailFromName(updates.getEmailFromName())
                .emailFromAddress(updates.getEmailFromAddress())
                .smsBody(updates.getSmsBody())
                .pushTitle(updates.getPushTitle())
                .pushBody(updates.getPushBody())
                .pushIconUrl(updates.getPushIconUrl())
                .pushImageUrl(updates.getPushImageUrl())
                .pushActionUrl(updates.getPushActionUrl())
                .inAppTitle(updates.getInAppTitle())
                .inAppBody(updates.getInAppBody())
                .inAppIcon(updates.getInAppIcon())
                .inAppActionUrl(updates.getInAppActionUrl())
                .inAppActionLabel(updates.getInAppActionLabel())
                .isActive(true)
                .priority(updates.getPriority() != null ? updates.getPriority() : existing.getPriority())
                .languageCode(existing.getLanguageCode())
                .version(existing.getVersion() + 1)
                .variables(updates.getVariables())
                .templateEngine(updates.getTemplateEngine() != null ? updates.getTemplateEngine() : existing.getTemplateEngine())
                .maxRetries(updates.getMaxRetries() != null ? updates.getMaxRetries() : existing.getMaxRetries())
                .retryIntervalMinutes(updates.getRetryIntervalMinutes() != null ? updates.getRetryIntervalMinutes() : existing.getRetryIntervalMinutes())
                .rateLimitPerUserHour(updates.getRateLimitPerUserHour())
                .rateLimitPerUserDay(updates.getRateLimitPerUserDay())
                .createdBy(existing.getCreatedBy())
                .updatedBy(updates.getUpdatedBy())
                .build();
        
        // Deactivate old version
        existing.setIsActive(false);
        templateRepository.save(existing);
        
        return templateRepository.save(newVersion);
    }
    
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getAllActiveTemplates() {
        return templateRepository.findByIsActiveTrue();
    }
    
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplatesByType(Notification.NotificationType type) {
        return templateRepository.findByNotificationTypeAndIsActiveTrue(type);
    }
    
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplateVersionHistory(String templateCode) {
        return templateRepository.findTemplateVersionHistory(templateCode);
    }
    
    @Transactional
    public void deactivateTemplate(UUID templateId) {
        templateRepository.findById(templateId).ifPresent(template -> {
            template.setIsActive(false);
            templateRepository.save(template);
        });
    }
    
    private Notification.NotificationType getNotificationTypeFromCode(String templateCode) {
        // Parse template code to determine notification type
        // Example: "USER_REGISTERED_EMAIL" -> USER_REGISTERED
        String typeStr = templateCode.split("_EMAIL|_SMS|_PUSH|_INAPP")[0];
        try {
            return Notification.NotificationType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            log.warn("Could not determine notification type from template code: {}", templateCode);
            return null;
        }
    }
}
