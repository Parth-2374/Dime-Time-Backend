package com.dimetime.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String subject, String body) {

        try {

            System.out.println("===== EMAIL DEBUG START =====");
            System.out.println("TO: " + to);
            System.out.println("SUBJECT: " + subject);

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom("newgamer0000007@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            System.out.println("EMAIL SENT SUCCESSFULLY");
            System.out.println("===== EMAIL DEBUG END =====");

        } catch (Exception e) {

            System.out.println("EMAIL FAILED");
            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }
}