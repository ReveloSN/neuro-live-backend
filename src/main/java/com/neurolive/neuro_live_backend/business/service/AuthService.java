package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.infrastructure.security.JwtService;
import com.neurolive.neuro_live_backend.presentation.dto.LoginRequest;
import com.neurolive.neuro_live_backend.presentation.dto.LoginResponse;
import com.neurolive.neuro_live_backend.presentation.dto.RegisterRequest;
import com.neurolive.neuro_live_backend.presentation.dto.RegisterResponse;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
// Orquesta el registro y el inicio de sesion de usuarios.
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.createForRole(request.getRole());
        user.register(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                "User registered successfully"
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        String token = user.authenticate(
                request.getPassword(),
                rawPassword -> passwordEncoder.matches(rawPassword, user.getPasswordHash()),
                () -> jwtService.generateToken(user)
        );

        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                token,
                "Login successful"
        );
    }
}
