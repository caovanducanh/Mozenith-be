package com.example.demologin.serviceImpl;

import com.example.demologin.exception.exceptions.InternalServerErrorException;
import com.example.demologin.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@Slf4j
@AllArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new InternalServerErrorException("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public void sendEmailOnBehalf(String fromName, String toEmail, String subject, String message, String ccEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // Resolve the system email address for the From field
            String systemEmail = (mailSender instanceof JavaMailSenderImpl impl)
                    ? impl.getUsername()
                    : "noreply@mozenith.com";

            helper.setFrom(new InternetAddress(systemEmail, fromName + " via Mozenith"));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(message);

            if (ccEmail != null && !ccEmail.isBlank()) {
                helper.setCc(ccEmail);
            }

            mailSender.send(mimeMessage);
            log.info("Email sent on behalf of '{}' to: {}", fromName, toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email on behalf of '{}' to: {}", fromName, toEmail, e);
            throw new InternalServerErrorException("Failed to send email: " + e.getMessage());
        }
    }
}
