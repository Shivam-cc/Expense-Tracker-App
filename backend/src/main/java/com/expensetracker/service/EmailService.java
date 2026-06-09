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
 * Sends OTP emails via the SendGrid Web API v3.
 * Free tier: 100 emails/day. No custom domain needed — just verify a sender email.
 *
 * Setup:
 *  1. Sign up at sendgrid.com
 *  2. Settings → Sender Authentication → Single Sender Verification → add your email
 *  3. Click the verification link sent to that email
 *  4. Settings → API Keys → Create API Key (Full Access) → copy it
 *  5. Set in Railway: SENDGRID_API_KEY and SENDGRID_FROM (the verified email)
 */
@Service
@Slf4j
public class EmailService {

    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    @Value("${app.sendgrid.api-key:}")
    private String apiKey;

    @Value("${app.sendgrid.from:noreply@expenseflow.com}")
    private String fromEmail;

    @Value("${app.sendgrid.from-name:ExpenseFlow}")
    private String fromName;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Sends OTP email. Returns true if sent, false if delivery failed (non-fatal).
     * OTP is always logged to console/Railway logs as a fallback.
     */
    public boolean sendOtpEmail(String toEmail, String otp, String name) {
        log.info("[OTP] *** code for {} => {} ***", toEmail, otp);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[OTP] SENDGRID_API_KEY not set — OTP only in logs above.");
            return false;
        }

        String body = """
                {
                  "personalizations": [{"to": [{"email": "%s", "name": "%s"}]}],
                  "from": {"email": "%s", "name": "%s"},
                  "subject": "Your ExpenseFlow Verification Code",
                  "content": [{"type": "text/html", "value": "%s"}]
                }
                """.formatted(
                escapeJson(toEmail), escapeJson(name),
                escapeJson(fromEmail), escapeJson(fromName),
                escapeJson(buildHtml(name, otp))
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SENDGRID_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            // SendGrid returns 202 Accepted on success
            if (response.statusCode() == 202) {
                log.info("[OTP] Email sent to {} via SendGrid", toEmail);
                return true;
            } else {
                log.error("[OTP] SendGrid error {} — OTP is in logs above. Body: {}",
                        response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[OTP] Email delivery failed: {} — OTP is in logs above.", e.getMessage());
            return false;
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String buildHtml(String name, String otp) {
        return "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;"
                + "background:#f9fafb;padding:32px;border-radius:12px'>"
                + "<h2 style='color:#2563eb;margin:0 0 8px'>ExpenseFlow</h2>"
                + "<p style='color:#374151'>Hi " + escapeJson(name) + ",</p>"
                + "<p style='color:#374151'>Your sign-up verification code is:</p>"
                + "<div style='font-size:38px;font-weight:700;letter-spacing:14px;color:#1e40af;"
                + "background:#dbeafe;padding:20px;border-radius:8px;text-align:center;margin:20px 0'>"
                + otp + "</div>"
                + "<p style='color:#6b7280;font-size:14px'>This code expires in "
                + "<strong>5 minutes</strong>. Never share it with anyone.</p>"
                + "<hr style='border:none;border-top:1px solid #e5e7eb;margin:24px 0'>"
                + "<p style='color:#9ca3af;font-size:12px'>"
                + "If you did not request this, you can safely ignore this email.</p></div>";
    }
}
