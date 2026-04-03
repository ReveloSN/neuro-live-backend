package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.user.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// Repositorio JPA para persistir y consultar registros de auditoria.
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByUserIdOrderByTimestampDesc(Long userId);

    List<AuditLog> findAllByTargetPatientIdOrderByTimestampDesc(Long targetPatientId);

    List<AuditLog> findAllByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
