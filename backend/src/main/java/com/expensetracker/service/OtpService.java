package com.expensetracker.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates, stores, and verifies time-limited 6-digit OTPs.
 * Storage is in-memory; entries expire after OTP_EXPIRY_SECONDS.
 */
@Service
public class OtpService {

    private static final int OTP_EXPIRY_SECONDS = 300;   // 5 minutes
    private static final int MAX_ATTEMPTS = 5;

    private record OtpEntry(String otp, Instant expiry, AtomicInteger attempts) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** Generates a new OTP for the given email and returns it. Overwrites any existing entry. */
    public String generateAndStore(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(),
                new OtpEntry(otp, Instant.now().plusSeconds(OTP_EXPIRY_SECONDS), new AtomicInteger(0)));
        return otp;
    }

    /**
     * Verifies the OTP for the given email.
     * Returns {@code true} if valid; removes entry on success or expiry.
     * Throws {@link RuntimeException} if max attempts exceeded.
     */
    public boolean verify(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;

        if (Instant.now().isAfter(entry.expiry())) {
            store.remove(email.toLowerCase());
            return false;
        }

        int attempts = entry.attempts().incrementAndGet();
        if (attempts > MAX_ATTEMPTS) {
            store.remove(email.toLowerCase());
            throw new RuntimeException("Too many OTP attempts. Please request a new code.");
        }

        if (!entry.otp().equals(otp)) {
            return false;
        }

        store.remove(email.toLowerCase());
        return true;
    }

    /** Returns {@code true} if a non-expired OTP entry exists for the given email. */
    public boolean hasValidOtp(String email) {
        OtpEntry entry = store.get(email.toLowerCase());
        return entry != null && Instant.now().isBefore(entry.expiry());
    }
}
