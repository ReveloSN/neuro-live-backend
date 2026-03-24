package com.neurolive.neuro_live_backend.controller;

import com.neurolive.neuro_live_backend.dto.LoginRequest;
import com.neurolive.neuro_live_backend.dto.LoginResponse;
import com.neurolive.neuro_live_backend.dto.RegisterRequest;
import com.neurolive.neuro_live_backend.dto.RegisterResponse;
import com.neurolive.neuro_live_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}