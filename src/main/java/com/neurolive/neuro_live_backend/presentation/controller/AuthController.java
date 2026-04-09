package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.AccountRecoveryService;
import com.neurolive.neuro_live_backend.business.service.AuthService;
import com.neurolive.neuro_live_backend.presentation.dto.AccountRecoveryRequestDTO;
import com.neurolive.neuro_live_backend.presentation.dto.AccountRecoveryResetDTO;
import com.neurolive.neuro_live_backend.presentation.dto.AccountRecoveryResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.AccountRecoveryValidationDTO;
import com.neurolive.neuro_live_backend.presentation.dto.LoginRequest;
import com.neurolive.neuro_live_backend.presentation.dto.LoginResponse;
import com.neurolive.neuro_live_backend.presentation.dto.RegisterRequest;
import com.neurolive.neuro_live_backend.presentation.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
// Expone los endpoints de autenticacion como registro e inicio de sesion.
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService;

    public AuthController(AuthService authService, AccountRecoveryService accountRecoveryService) {
        this.authService = authService;
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/account-recovery/request")
    public ResponseEntity<AccountRecoveryResponseDTO> requestAccountRecovery(
            @Valid @RequestBody AccountRecoveryRequestDTO request) {
        return ResponseEntity.accepted().body(
                AccountRecoveryResponseDTO.from(accountRecoveryService.requestRecovery(request.email()))
        );
    }

    @PostMapping("/account-recovery/validate")
    public ResponseEntity<AccountRecoveryResponseDTO> validateAccountRecovery(
            @Valid @RequestBody AccountRecoveryValidationDTO request) {
        return ResponseEntity.ok(
                AccountRecoveryResponseDTO.from(accountRecoveryService.validateToken(request.email(), request.token()))
        );
    }

    @PostMapping("/account-recovery/reset")
    public ResponseEntity<AccountRecoveryResponseDTO> resetPassword(
            @Valid @RequestBody AccountRecoveryResetDTO request) {
        accountRecoveryService.resetPassword(request.email(), request.token(), request.newPassword());
        return ResponseEntity.ok(new AccountRecoveryResponseDTO("Password updated successfully", null, true));
    }
}
