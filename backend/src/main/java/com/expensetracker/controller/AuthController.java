package com.expensetracker.controller;

import com.expensetracker.dto.AuthRequest;
import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.KeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KeyService keyService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Returns the RSA public key so the browser can encrypt passwords before sending. */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", keyService.getPublicKeyBase64()));
    }
}
