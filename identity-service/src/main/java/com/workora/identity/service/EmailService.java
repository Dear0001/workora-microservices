package com.workora.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationOtp(String recipient, String otp) {
        send(recipient, "Workora email verification code",
                "Your Workora email verification code is: " + otp
                        + "\n\nThis code expires in 10 minutes.");
    }

    public void sendPasswordResetOtp(String recipient, String otp) {
        send(recipient, "Workora password reset code",
                "Your Workora password reset code is: " + otp
                        + "\n\nThis code expires in 10 minutes. If you did not request this, ignore this email.");
    }

    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
