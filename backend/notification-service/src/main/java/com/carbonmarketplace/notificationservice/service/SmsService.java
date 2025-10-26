package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.dto.NotificationMessage;
import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationTemplate;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {
    
    private final NotificationService notificationService;
    private final TemplateService templateService;
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.from-number}")
    private String fromNumber;
    
    @Value("${twilio.enabled:true}")
    private boolean twilioEnabled;
    
    @PostConstruct
    public void init() {
        if (twilioEnabled) {
            Twilio.init(accountSid, authToken);
        }
    }
    
    @RabbitListener(queues = "${rabbitmq.queue.sms}")
    public void processSmsNotification(NotificationMessage message) {
        log.info("Processing SMS notification: {} for user: {}", 
                message.getNotificationId(), message.getUserId());
        
        try {
            // Validate phone number
            if (message.getRecipientPhone() == null || message.getRecipientPhone().isEmpty()) {
                throw new IllegalArgumentException("Recipient phone number is required");
            }
            
            // Get message content
            String smsContent = message.getMessage();
            
            if (message.getTemplateCode() != null) {
                NotificationTemplate template = templateService.getTemplate(
                        message.getTemplateCode(),
                        "en", // TODO: Get user's language preference
                        Notification.NotificationChannel.SMS);
                
                if (template != null && template.getSmsBody() != null) {
                    Map<String, Object> variables = message.getTemplateVariables();
                    smsContent = templateService.processTemplate(template.getSmsBody(), variables);
                }
            }
            
            // Ensure SMS length is within limits (160 characters for single SMS)
            if (smsContent.length() > 1600) {
                smsContent = smsContent.substring(0, 1597) + "...";
            }
            
            boolean sent = false;
            String externalId = null;
            
            if (twilioEnabled) {
                externalId = sendViaTwilio(message.getRecipientPhone(), smsContent);
                sent = externalId != null;
            } else {
                // For testing/development, just log the SMS
                log.info("SMS (Mock): To: {}, Message: {}", message.getRecipientPhone(), smsContent);
                sent = true;
            }
            
            if (sent) {
                notificationService.updateDeliveryStatus(
                        message.getNotificationId(),
                        Notification.DeliveryStatus.SENT,
                        null);
                log.info("SMS sent successfully to: {}", message.getRecipientPhone());
            } else {
                throw new RuntimeException("Failed to send SMS");
            }
            
        } catch (Exception e) {
            log.error("Failed to send SMS notification: {}", message.getNotificationId(), e);
            handleFailure(message, e);
        }
    }
    
    private String sendViaTwilio(String toNumber, String messageContent) {
        try {
            // Format phone number if needed
            if (!toNumber.startsWith("+")) {
                // Assume Vietnam if no country code
                if (!toNumber.startsWith("84")) {
                    toNumber = "+84" + toNumber.replaceFirst("^0", "");
                } else {
                    toNumber = "+" + toNumber;
                }
            }
            
            Message twilioMessage = Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(fromNumber),
                    messageContent
            ).create();
            
            log.debug("Twilio message SID: {}, Status: {}", 
                    twilioMessage.getSid(), twilioMessage.getStatus());
            
            return twilioMessage.getSid();
            
        } catch (Exception e) {
            log.error("Twilio send error", e);
            return null;
        }
    }
    
    private void handleFailure(NotificationMessage message, Exception e) {
        // Check if retry is needed
        if (message.getRetryCount() < message.getMaxRetries()) {
            // SMS retries should be more conservative
            log.info("Requeueing SMS notification {} for retry (attempt {} of {})",
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
