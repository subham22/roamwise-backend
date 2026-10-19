package com.roamwise.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    // e.g. "Roamwise <noreply@roamwise.live>"; must be a verified Brevo sender
    @Value("${app.mail.from}")
    private String from;

    @Async
    public void sendShareNotification(String toEmail, String tripDestination, String shareUrl) throws MessagingException {
        send(toEmail,
                "Someone shared a trip with you: " + tripDestination,
                "email/trip-shared",
                Map.of("tripDestination", tripDestination, "shareUrl", shareUrl),
                "Someone shared a trip to " + tripDestination + " with you.\n\nView it here: " + shareUrl);
    }

    @Async
    public void sendWelcomeEmail(String toEmail) throws MessagingException {
        send(toEmail,
                "Welcome to Roamwise",
                "email/welcome",
                Map.of("plannerUrl", "https://roamwise.live/app/generate",
                        "guidesUrl", "https://roamwise.live/guides"),
                "Welcome to Roamwise!\n\n"
                        + "Plan your first trip: https://roamwise.live/app/generate\n"
                        + "Browse ready-made itineraries: https://roamwise.live/guides");
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) throws MessagingException {
        send(toEmail,
                "Reset your Roamwise password",
                "email/password-reset",
                Map.of("resetLink", resetLink),
                "Click this link to reset your password: " + resetLink
                        + "\n\nThis link expires in 1 hour. If you didn't request this, ignore this email.");
    }

    private void send(String to, String subject, String template, Map<String, Object> vars, String plainText) throws MessagingException {

        Context context = new Context();
        context.setVariables(vars);

        String html = templateEngine.process(template, context);

        MimeMessage msg = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plainText, html);
        try {
            mailSender.send(msg);
        } catch (MailException e) {
            log.error("Failed to send email '{}' to {}", subject, to, e);
        }
    }
}