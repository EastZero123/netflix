package com.netflix.clone.serviceImpl;

import com.netflix.clone.exception.EmailNotVerifiedException;
import com.netflix.clone.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Netflix - Verify your Email");

            String verificationLink = frontendUrl + "/verify-email?token=" + token;

            String emailBody =
                    "Welcome to Netflix!\n\n"
                    + "Thank you for registering. Please verify your email address by clicking the link below:\n\n"
                    + verificationLink
                    + "\n\n"
                    + "This link will expire in 24 hours.\n\n"
                    + "If you didn't create this account, please ignore this email. \n\n"
                    + "Danke";

            message.setText(emailBody);
            mailSender.send(message);
            logger.info("Email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Error sending email to {}", toEmail, e);
            throw new EmailNotVerifiedException("Error sending email to " + toEmail + ":");
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Netflix - Reset your Password");

            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String emailBody =
                    "Hi,\n\n"
                    + "We received a request to reset your password. Please click the link below to reset your password:\n\n"
                    + resetLink
                    + "\n\n"
                    + "This link will expire in 1 hours.\n\n"
                    + "If you didn't request this, please ignore this email.\n\n"
                    + "Danke";

            message.setText(emailBody);
            mailSender.send(message);
            logger.info("Email sent to {}", toEmail);

        } catch (Exception e) {
            logger.error("Error sending email to {}", toEmail, e);
            throw new RuntimeException("Error sending email to " + toEmail + ":");
        }
    }
}
