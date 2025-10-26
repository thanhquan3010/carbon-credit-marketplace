package com.carbonmarketplace.userservice.service;

import com.carbonmarketplace.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from:noreply@carbonmarketplace.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(User user) {
        try {
            String verificationToken = generateVerificationToken(user.getUserId());
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;

            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getFullName());
            variables.put("verificationUrl", verificationUrl);

            sendHtmlEmail(
                    user.getEmail(),
                    "Verify Your Email Address",
                    "email-verification",
                    variables);

            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getFullName());
            variables.put("resetUrl", resetUrl);

            sendHtmlEmail(
                    user.getEmail(),
                    "Reset Your Password",
                    "password-reset",
                    variables);

            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getFullName());
            variables.put("dashboardUrl", frontendUrl + "/dashboard");

            sendHtmlEmail(
                    user.getEmail(),
                    "Welcome to Carbon Credit Marketplace",
                    "welcome",
                    variables);

            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendKycApprovedEmail(User user) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getFullName());
            variables.put("dashboardUrl", frontendUrl + "/dashboard");

            sendHtmlEmail(
                    user.getEmail(),
                    "KYC Verification Approved",
                    "kyc-approved",
                    variables);

            log.info("KYC approved email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send KYC approved email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendKycRejectedEmail(User user, String reason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getFullName());
            variables.put("rejectionReason", reason);
            variables.put("kycUrl", frontendUrl + "/kyc");

            sendHtmlEmail(
                    user.getEmail(),
                    "KYC Verification Rejected",
                    "kyc-rejected",
                    variables);

            log.info("KYC rejected email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send KYC rejected email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendTwoFactorCode(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Your Two-Factor Authentication Code");
            message.setText(String.format(
                    "Your 2FA code is: %s\n\nThis code will expire in 5 minutes.\n\n" +
                            "If you didn't request this code, please ignore this email.",
                    code));

            mailSender.send(message);
            log.info("2FA code sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send 2FA code to: {}", email, e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        Context context = new Context();
        context.setVariables(variables);

        // For now, send plain text since we don't have templates set up
        // In production, you would use: templateEngine.process(templateName, context)
        String htmlContent = generatePlainHtmlContent(templateName, variables);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String generatePlainHtmlContent(String templateName, Map<String, Object> variables) {
        String userName = (String) variables.getOrDefault("userName", "User");

        return switch (templateName) {
            case "email-verification" -> String.format(
                    "<html><body>" +
                            "<h2>Hello %s,</h2>" +
                            "<p>Please verify your email address by clicking the link below:</p>" +
                            "<p><a href='%s'>Verify Email</a></p>" +
                            "<p>If you didn't create an account, please ignore this email.</p>" +
                            "</body></html>",
                    userName, variables.get("verificationUrl"));
            case "password-reset" -> String.format(
                    "<html><body>" +
                            "<h2>Hello %s,</h2>" +
                            "<p>You requested a password reset. Click the link below to reset your password:</p>" +
                            "<p><a href='%s'>Reset Password</a></p>" +
                            "<p>If you didn't request this, please ignore this email.</p>" +
                            "</body></html>",
                    userName, variables.get("resetUrl"));
            case "welcome" -> String.format(
                    "<html><body>" +
                            "<h2>Welcome %s!</h2>" +
                            "<p>Thank you for joining Carbon Credit Marketplace.</p>" +
                            "<p>Get started by visiting your <a href='%s'>dashboard</a>.</p>" +
                            "</body></html>",
                    userName, variables.get("dashboardUrl"));
            case "kyc-approved" -> String.format(
                    "<html><body>" +
                            "<h2>Congratulations %s!</h2>" +
                            "<p>Your KYC verification has been approved.</p>" +
                            "<p>You can now access all features in your <a href='%s'>dashboard</a>.</p>" +
                            "</body></html>",
                    userName, variables.get("dashboardUrl"));
            case "kyc-rejected" -> String.format(
                    "<html><body>" +
                            "<h2>Hello %s,</h2>" +
                            "<p>Unfortunately, your KYC verification was rejected.</p>" +
                            "<p>Reason: %s</p>" +
                            "<p>Please <a href='%s'>resubmit your KYC documents</a>.</p>" +
                            "</body></html>",
                    userName, variables.get("rejectionReason"), variables.get("kycUrl"));
            default -> "<html><body><p>Email notification</p></body></html>";
        };
    }

    private String generateVerificationToken(UUID userId) {
        // In production, this would generate a secure token and store it
        return UUID.randomUUID().toString();
    }
}
