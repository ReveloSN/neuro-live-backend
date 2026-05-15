package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.AuditLog;
import com.neurolive.neuro_live_backend.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
// Registra y consulta eventos de auditoria para trazabilidad del sistema.
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog record(Long userId, String action, Long targetPatientId, String ipOrigin) {
        AuditLog auditLog = new AuditLog();
        auditLog.record(userId, action, targetPatientId, ipOrigin);
        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByUser(Long userId) {
        return auditLogRepository.findAllByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByUser(Long userId, int page, int size) {
        return auditLogRepository.findAllByUserIdOrderByTimestampDesc(userId, PageRequest.of(page, clampSize(size)));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByPatient(Long targetPatientId) {
        return auditLogRepository.findAllByTargetPatientIdOrderByTimestampDesc(targetPatientId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByPatient(Long targetPatientId, int page, int size) {
        return auditLogRepository.findAllByTargetPatientIdOrderByTimestampDesc(targetPatientId, PageRequest.of(page, clampSize(size)));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Date range is required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after the start date");
        }
        return auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByDateRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Date range is required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after the start date");
        }
        return auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(start, end, PageRequest.of(page, clampSize(size)));
    }

    private int clampSize(int size) {
        return Math.min(Math.max(1, size), 200);
    }
}
