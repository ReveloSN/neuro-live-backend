package com.neurolive.neuro_live_backend.business.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@ConditionalOnProperty(prefix = "account-recovery.mail", name = "mode", havingValue = "logging", matchIfMissing = true)
public class LoggingAccountRecoveryMailService implements AccountRecoveryMailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAccountRecoveryMailService.class);

    @Override
    public void sendRecoveryToken(String email, String code, LocalDateTime expiresAt) {
        LOGGER.info("Account recovery token email={} code={} expiresAt={}", email, maskRecoveryCode(code), expiresAt);
    }

    private static String maskRecoveryCode(String code) {
        if (code == null || code.length() < 2) {
            return "******";
        }
        return code.substring(0, 2) + "****";
    }
}
