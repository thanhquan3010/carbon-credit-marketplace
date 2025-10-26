package com.carbonmarketplace.notificationservice.service;

import com.carbonmarketplace.notificationservice.dto.NotificationMessage;
import com.carbonmarketplace.notificationservice.entity.Notification;
import com.carbonmarketplace.notificationservice.entity.NotificationTemplate;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final NotificationService notificationService;
    private final TemplateService templateService;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    
    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.enabled:true}")
    private boolean sendGridEnabled;
    
    @Value("${email.from.address}")
    private String defaultFromAddress;
    
    @Value("${email.from.name}")
    private String defaultFromName;
    
    @Value("${email.reply-to}")
    private String replyToAddress;
    
    @RabbitListener(queues = "${rabbitmq.queue.email}")
    public void processEmailNotification(NotificationMessage message) {
        log.info("Processing email notification: {} for user: {}", 
                message.getNotificationId(), message.getUserId());
        
        try {
            // Get template if specified
            String subject = message.getTitle();
            String htmlContent = message.getMessage();
            String textContent = message.getMessage();
            
            if (message.getTemplateCode() != null) {
                NotificationTemplate template = templateService.getTemplate(
                        message.getTemplateCode(), 
                        "en", // TODO: Get user's language preference
                        Notification.NotificationChannel.EMAIL);
                
                if (template != null) {
                    Map<String, Object> variables = message.getTemplateVariables();
                    subject = templateService.processTemplate(template.getEmailSubject(), variables);
                    htmlContent = templateService.processTemplate(template.getEmailBodyHtml(), variables);
                    textContent = templateService.processTemplate(template.getEmailBodyText(), variables);
                }
            }
            
            boolean sent;
            String externalId = null;
            
            if (sendGridEnabled) {
                sent = sendViaSendGrid(message.getRecipientEmail(), subject, htmlContent, textContent);
                // TODO: Extract SendGrid message ID
            } else {
                sent = sendViaSmtp(message.getRecipientEmail(), subject, htmlContent);
            }
            
            if (sent) {
                notificationService.updateDeliveryStatus(
                        message.getNotificationId(), 
                        Notification.DeliveryStatus.SENT, 
                        null);
                log.info("Email sent successfully to: {}", message.getRecipientEmail());
            } else {
                throw new RuntimeException("Failed to send email");
            }
            
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", message.getNotificationId(), e);
            handleFailure(message, e);
        }
    }
    
    private boolean sendViaSendGrid(String to, String subject, String htmlContent, String textContent) {
        try {
            Email from = new Email(defaultFromAddress, defaultFromName);
            Email toEmail = new Email(to);
            Content htmlContentObj = new Content("text/html", htmlContent);
            Content textContentObj = new Content("text/plain", textContent);
            
            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setSubject(subject);
            
            Personalization personalization = new Personalization();
            personalization.addTo(toEmail);
            mail.addPersonalization(personalization);
            
            mail.addContent(textContentObj);
            mail.addContent(htmlContentObj);
            
            if (replyToAddress != null) {
                mail.setReplyTo(new Email(replyToAddress));
            }
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.debug("SendGrid response: {}", response.getBody());
                return true;
            } else {
                log.error("SendGrid error: {} - {}", response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (IOException e) {
            log.error("SendGrid API error", e);
            return false;
        }
    }
    
    private boolean sendViaSmtp(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(defaultFromAddress, defaultFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            if (replyToAddress != null) {
                helper.setReplyTo(replyToAddress);
            }
            
            mailSender.send(message);
            return true;
            
        } catch (Exception e) {
            log.error("SMTP send error", e);
            return false;
        }
    }
    
    private void handleFailure(NotificationMessage message, Exception e) {
        // Check if retry is needed
        if (message.getRetryCount() < message.getMaxRetries()) {
            // Requeue for retry
            log.info("Requeueing email notification {} for retry (attempt {} of {})",
                    message.getNotificationId(),
                    message.getRetryCount() + 1,
                    message.getMaxRetries());
            // TODO: Implement retry logic with exponential backoff
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
