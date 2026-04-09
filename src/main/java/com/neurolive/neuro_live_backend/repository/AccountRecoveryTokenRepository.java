package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.user.AccountRecoveryToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRecoveryTokenRepository extends JpaRepository<AccountRecoveryToken, Long> {

    List<AccountRecoveryToken> findAllByUserIdAndConsumedAtIsNullAndExpiresAtAfter(Long userId, LocalDateTime referenceTime);

    Optional<AccountRecoveryToken> findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String email,
            LocalDateTime referenceTime
    );
}
