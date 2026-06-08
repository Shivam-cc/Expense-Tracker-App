package com.expensetracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OTP delivery abstraction.
 *
 * For now this runs in dev-safe mode and logs the OTP so sign-up can be tested
 * without SMTP credentials.
 */
@Service
@Slf4j
public class EmailService {

  public void sendOtpEmail(String toEmail, String otp, String name) {
    log.info("[OTP] Verification code for {} ({}) => {}", name, toEmail, otp);
  }
}
