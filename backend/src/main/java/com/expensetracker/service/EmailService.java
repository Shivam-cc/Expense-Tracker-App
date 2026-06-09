package com.expensetracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends OTP emails via the Resend HTTP API (https://resend.com).
 *
 * No SMTP, no Gmail auth issues, works from any cloud provider.
 * Free tier: 3,000 emails / month, 100 / day.
 *
 * Set RESEND_API_KEY in Railway env vars to enable real email delivery.
 * When the key is absent, the OTP is logged to the console only (dev mode).
 */
@Service
@Slf4j
public class EmailService {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    @Value("${app.resend.api-key:}")
    private String apiKey;

    @Value("${app.resend.from:onboarding@resend.dev}")
    private String fromEmail;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendOtpEmail(String toEmail, String otp, String name) {
        log.info("[OTP] *** code for {} => {} ***", toEmail, otp);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[OTP] RESEND_API_KEY not set — OTP only in logs above.");
            return false;
        }

        String body = """
                {
                  "from": "%s",
                  "to": ["%s"],
                  "subject": "Your ExpenseFlow Verification Code",
                  "html": %s
                }
                """.formatted(fromEmail, toEmail, jsonString(buildHtml(name, otp)));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[OTP] Email sent to {} via Resend", toEmail);
                return true;
            } else {
                log.error("[OTP] Resend API error {} — OTP is in logs above. Body: {}",
                        response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[OTP] Email delivery failed: {} — OTP is in logs above.", e.getMessage());
            return false;
        }
    }

    /** Escapes a string for safe embedding as a JSON string value. */
    private static String jsonString(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private String buildHtml(String name, String otp) {
        return """
               <div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;\
background:#f9fafb;padding:32px;border-radius:12px'>\
<h2 style='color:#2563eb;margin:0 0 8px'>ExpenseFlow</h2>\
<p style='color:#374151'>Hi %s,</p>\
<p style='color:#374151'>Your sign-up verification code is:</p>\
<div style='font-size:38px;font-weight:700;letter-spacing:14px;color:#1e40af;\
background:#dbeafe;padding:20px;border-radius:8px;text-align:center;margin:20px 0'>%s</div>\
<p style='color:#6b7280;font-size:14px'>This code expires in <strong>5 minutes</strong>.\
 Never share it with anyone.</p>\
<hr style='border:none;border-top:1px solid #e5e7eb;margin:24px 0'>\
<p style='color:#9ca3af;font-size:12px'>\
If you did not request this, you can safely ignore this email.</p></div>\
               """.formatted(name, otp);
    }
}
