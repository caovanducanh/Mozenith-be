package com.example.demologin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

public interface EmailService {
    void sendEmail(String to, String subject, String text);
    void sendEmailOnBehalf(String fromName, String toEmail, String subject, String message, String ccEmail);
}
