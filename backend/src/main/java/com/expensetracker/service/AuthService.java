package com.expensetracker.service;

import com.expensetracker.dto.AuthRequest;
import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.dto.SendOtpRequest;
import com.expensetracker.model.Role;
import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KeyService keyService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.otp.dev-mode:false}")
    private boolean devOtpMode;

    /**
     * Validates the email, generates an OTP, and sends it.
     * In dev mode the OTP is also returned in the response so you can test without email.
     */
    public Map<String, Object> sendOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        String otp = otpService.generateAndStore(email);
        boolean emailSent = emailService.sendOtpEmail(email, otp, request.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Verification code sent to " + email);
        if (devOtpMode || !emailSent) response.put("devOtp", otp);
        return response;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Verify OTP before creating the account
        if (!otpService.verify(email, request.getOtp())) {
            throw new RuntimeException("Invalid or expired verification code");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        // Decrypt RSA-OAEP envelope → plain SHA-256 hash from browser
        String decodedPassword;
        try {
            decodedPassword = keyService.decrypt(request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException("Invalid password encoding");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(decodedPassword))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Decrypt RSA-OAEP envelope → plain SHA-256 hash from browser
        String decodedPassword;
        try {
            decodedPassword = keyService.decrypt(request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException("Invalid password encoding");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, decodedPassword)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
