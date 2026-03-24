package com.neurolive.neuro_live_backend.service;

import com.neurolive.neuro_live_backend.dto.LoginRequest;
import com.neurolive.neuro_live_backend.dto.LoginResponse;
import com.neurolive.neuro_live_backend.dto.RegisterRequest;
import com.neurolive.neuro_live_backend.dto.RegisterResponse;
import com.neurolive.neuro_live_backend.entity.Role;
import com.neurolive.neuro_live_backend.entity.User;
import com.neurolive.neuro_live_backend.repository.RoleRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                    RoleRepository roleRepository,
                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().getName().name(),
                "User registered successfully"
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("User is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new RuntimeException("Invalid credentials");
        }

        return new LoginResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole().getName().name(),
            "Login successful"
        );
    }
}