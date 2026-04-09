package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.AccountRecoveryToken;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.AccountRecoveryTokenRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
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

    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final String GENERIC_RECOVERY_MESSAGE =
            "If the email exists, recovery instructions were sent";

    private final UserRepository userRepository;
    private final AccountRecoveryTokenRepository accountRecoveryTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRecoveryMailService accountRecoveryMailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountRecoveryService(UserRepository userRepository,
                                  AccountRecoveryTokenRepository accountRecoveryTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  AccountRecoveryMailService accountRecoveryMailService) {
        this.userRepository = userRepository;
        this.accountRecoveryTokenRepository = accountRecoveryTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRecoveryMailService = accountRecoveryMailService;
    }

    public RecoveryStatus requestRecovery(String email) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);
        if (optionalUser.isEmpty()) {
            return new RecoveryStatus(GENERIC_RECOVERY_MESSAGE, null, false);
        }

        User user = optionalUser.get();
        LocalDateTime now = LocalDateTime.now();
        accountRecoveryTokenRepository.findAllByUserIdAndConsumedAtIsNullAndExpiresAtAfter(user.getId(), now)
                .forEach(token -> safeConsume(token, now));

        String rawToken = generateToken();
        AccountRecoveryToken accountRecoveryToken = AccountRecoveryToken.issue(
                user.getId(),
                user.getEmail(),
                hashToken(rawToken),
                now.plus(TOKEN_TTL)
        );
        accountRecoveryTokenRepository.save(accountRecoveryToken);
        accountRecoveryMailService.sendRecoveryToken(user.getEmail(), rawToken, accountRecoveryToken.getExpiresAt());

        return new RecoveryStatus("Recovery token generated", accountRecoveryToken.getExpiresAt(), true);
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

    public record RecoveryStatus(String message, LocalDateTime expiresAt, boolean valid) {
    }
}
