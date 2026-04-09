package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Persiste y consulta señales de dinamica de tecleo.
public interface KeystrokeDynamicsRepository extends JpaRepository<KeystrokeDynamics, Long> {

    List<KeystrokeDynamics> findAllByUserIdOrderByTimestampDesc(Long userId);

    List<KeystrokeDynamics> findAllByUserIdAndTimestampBetweenOrderByTimestampAsc(
            Long userId,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );

    java.util.Optional<KeystrokeDynamics> findTopByUserIdOrderByTimestampDesc(Long userId);
}
