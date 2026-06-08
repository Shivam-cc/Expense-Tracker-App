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

    @Value("${app.otp.dev-mode:true}")
    private boolean devOtpMode;

    /**
     * Validates the email, generates an OTP, and sends it.
     * In dev mode the OTP is also returned in the response so you can test without email.
     */
    public Map<String, Object> sendOtp(SendOtpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String otp = otpService.generateAndStore(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp, request.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Verification code sent to " + request.getEmail());
        if (devOtpMode) {
            response.put("devOtp", otp);   // only in dev — remove in production
        }
        return response;
    }

    public AuthResponse register(RegisterRequest request) {
        // Verify OTP before creating the account
        if (!otpService.verify(request.getEmail(), request.getOtp())) {
            throw new RuntimeException("Invalid or expired verification code");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
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
                .name(request.getName())
                .email(request.getEmail())
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
        // Decrypt RSA-OAEP envelope → plain SHA-256 hash from browser
        String decodedPassword;
        try {
            decodedPassword = keyService.decrypt(request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException("Invalid password encoding");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        decodedPassword
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
