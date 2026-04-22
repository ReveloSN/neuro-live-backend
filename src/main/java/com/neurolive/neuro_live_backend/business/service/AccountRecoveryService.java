package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.AccountRecoveryToken;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.AccountRecoveryTokenRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class AccountRecoveryService {

    private static final String GENERIC_RECOVERY_MESSAGE =
            "If the email exists, recovery instructions were sent";

    private final UserRepository userRepository;
    private final AccountRecoveryTokenRepository accountRecoveryTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRecoveryMailService accountRecoveryMailService;
    private final Duration tokenTtl;
    private final Duration requestCooldown;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountRecoveryService(UserRepository userRepository,
                                AccountRecoveryTokenRepository accountRecoveryTokenRepository,
                                PasswordEncoder passwordEncoder,
                                AccountRecoveryMailService accountRecoveryMailService,
                                @Value("${account-recovery.token-expiration-minutes:15}") long tokenExpirationMinutes,
                                @Value("${account-recovery.request-cooldown-seconds:60}") long requestCooldownSeconds) {
        this.userRepository = userRepository;
        this.accountRecoveryTokenRepository = accountRecoveryTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRecoveryMailService = accountRecoveryMailService;
        this.tokenTtl = validateMinutes(tokenExpirationMinutes, "Account recovery token expiration");
        this.requestCooldown = validateSeconds(requestCooldownSeconds, "Account recovery request cooldown");
    }

    public RecoveryStatus requestRecovery(String email) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);
        if (optionalUser.isEmpty()) {
            return genericRequestStatus();
        }

        User user = optionalUser.get();
        LocalDateTime now = LocalDateTime.now();
        Optional<AccountRecoveryToken> activeToken = accountRecoveryTokenRepository
                .findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        normalizedEmail,
                        now
                );

        if (activeToken.isPresent() && !activeToken.get().getCreatedAt().isBefore(now.minus(requestCooldown))) {
            return genericRequestStatus();
        }

        accountRecoveryTokenRepository.findAllByUserIdAndConsumedAtIsNullAndExpiresAtAfter(user.getId(), now)
                .forEach(token -> safeConsume(token, now));

        String rawToken = generateToken();
        AccountRecoveryToken accountRecoveryToken = AccountRecoveryToken.issue(
                user.getId(),
                user.getEmail(),
                hashToken(rawToken),
                now.plus(tokenTtl)
        );
        accountRecoveryTokenRepository.save(accountRecoveryToken);
        accountRecoveryMailService.sendRecoveryToken(user.getEmail(), rawToken, accountRecoveryToken.getExpiresAt());

        return genericRequestStatus();
    }

    @Transactional(readOnly = true)
    public RecoveryStatus validateToken(String email, String rawToken) {
        AccountRecoveryToken accountRecoveryToken = requireActiveToken(email, rawToken);
        return new RecoveryStatus("Recovery token is valid", accountRecoveryToken.getExpiresAt(), true);
    }

    public void resetPassword(String email, String rawToken, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }

        AccountRecoveryToken accountRecoveryToken = requireActiveToken(email, rawToken);
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Recovery token is invalid or expired"));

        user.recoverAccount(passwordEncoder.encode(newPassword));
        accountRecoveryToken.consume(LocalDateTime.now());
        userRepository.save(user);
        accountRecoveryTokenRepository.save(accountRecoveryToken);
    }

    private AccountRecoveryToken requireActiveToken(String email, String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        AccountRecoveryToken accountRecoveryToken = accountRecoveryTokenRepository
                .findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        normalizeEmail(email),
                        now
                )
                .orElseThrow(() -> new IllegalArgumentException("Recovery token is invalid or expired"));

        if (!accountRecoveryToken.isActive(now)) {
            throw new IllegalArgumentException("Recovery token is invalid or expired");
        }
        if (!accountRecoveryToken.matches(hashToken(rawToken))) {
            throw new IllegalArgumentException("Recovery token is invalid or expired");
        }

        return accountRecoveryToken;
    }

    private void safeConsume(AccountRecoveryToken token, LocalDateTime now) {
        if (token.isActive(now)) {
            token.consume(now);
            accountRecoveryTokenRepository.save(token);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] buffer = new byte[18];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Recovery token is required");
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available for token hashing", exception);
        }
    }

    private RecoveryStatus genericRequestStatus() {
        return new RecoveryStatus(GENERIC_RECOVERY_MESSAGE, null, true);
    }

    private Duration validateMinutes(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return Duration.ofMinutes(value);
    }

    private Duration validateSeconds(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return Duration.ofSeconds(value);
    }

    public record RecoveryStatus(String message, LocalDateTime expiresAt, boolean valid) {
    }
}
