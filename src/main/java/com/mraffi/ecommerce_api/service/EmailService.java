package com.mraffi.ecommerce_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

   private final JavaMailSender mailSender;

   @Value("${spring.client.url}")
   private String url;

   public void sendVerificationEmail(String toEmail, String token){
      String verificationLink = url + "/auth/verify-email?token=" + token;

      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(toEmail);
      message.setSubject("Email Verification");
      message.setText(
              "Click the link to verify your email:\n" + verificationLink
      );

      mailSender.send(message);
   }

}
