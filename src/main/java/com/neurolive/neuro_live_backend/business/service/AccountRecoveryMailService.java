package com.neurolive.neuro_live_backend.business.service;

import java.time.LocalDateTime;

public interface AccountRecoveryMailService {

    void sendRecoveryToken(String email, String rawToken, LocalDateTime expiresAt);
}
