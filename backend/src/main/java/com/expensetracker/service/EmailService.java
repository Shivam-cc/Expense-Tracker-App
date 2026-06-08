package com.expensetracker.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends OTP verification emails via SMTP.
 * When spring.mail.host is not configured (local dev), the OTP is only logged
 * to the console — no email is sent and the app still starts fine.
 */
@Service
@Slf4j
public class EmailService {

    /** Null when spring.mail.host is not set (local/unconfigured environments). */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@expenseflow.com}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp, String name) {
        // Always log for traceability (visible in Railway logs too)
        log.info("[OTP] code for {} → {}", toEmail, otp);

        if (mailSender == null) {
            log.warn("[OTP] No SMTP configured — email not sent. Set MAIL_HOST / MAIL_USERNAME / MAIL_PASSWORD env vars to enable.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your ExpenseFlow Verification Code");
            helper.setText(buildHtml(name, otp), true);
            mailSender.send(message);
            log.info("[OTP] Email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[OTP] Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    private String buildHtml(String name, String otp) {
        return """
               <div style="font-family:Arial,sans-serif;max-width:500px;margin:0 auto;
                           background:#f9fafb;padding:32px;border-radius:12px">
                 <h2 style="color:#2563eb;margin:0 0 8px">ExpenseFlow</h2>
                 <p style="color:#374151">Hi %s,</p>
                 <p style="color:#374151">Your sign-up verification code is:</p>
                 <div style="font-size:38px;font-weight:700;letter-spacing:14px;color:#1e40af;
                             background:#dbeafe;padding:20px;border-radius:8px;
                             text-align:center;margin:20px 0">
                   %s
                 </div>
                 <p style="color:#6b7280;font-size:14px">
                   This code expires in <strong>5 minutes</strong>. Never share it with anyone.
                 </p>
                 <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0">
                 <p style="color:#9ca3af;font-size:12px">
                   If you did not request this, you can safely ignore this email.
                 </p>
               </div>
               """.formatted(name, otp);
    }
}