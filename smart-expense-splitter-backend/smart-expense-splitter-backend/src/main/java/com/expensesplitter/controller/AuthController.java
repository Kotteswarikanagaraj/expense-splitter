package com.expensesplitter.controller;

import com.expensesplitter.dto.request.LoginRequest;
import com.expensesplitter.dto.request.RegisterRequest;
import com.expensesplitter.dto.response.AuthResponse;
import com.expensesplitter.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Public endpoint (permitAll in SecurityConfig) — this is how a user gets
    // their first JWT, before they have any credentials to authenticate with.
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
