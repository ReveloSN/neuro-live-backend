package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.AuditLogService;
import com.neurolive.neuro_live_backend.business.service.AuditLogService.ChainVerificationResult;
import com.neurolive.neuro_live_backend.business.service.ClinicalAccessService;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.presentation.dto.AuditLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/audit")
// Expone el historial de auditoria para doctores y pacientes con sus propios
// registros.
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final ClinicalAccessService clinicalAccessService;

    public AuditLogController(AuditLogService auditLogService, ClinicalAccessService clinicalAccessService) {
        this.auditLogService = auditLogService;
        this.clinicalAccessService = clinicalAccessService;
    }

    // Devuelve los registros de auditoria de un usuario. El propio usuario puede
    // ver los suyos; doctores pueden ver los de cualquiera.
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<AuditLogDTO>> getByUser(Authentication authentication,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        User requester = clinicalAccessService.resolveCurrentUser(authentication.getName());
        requireSelfOrDoctor(requester, userId);
        return ResponseEntity.ok(auditLogService.getByUser(userId, page, size).map(AuditLogDTO::from));
    }

    // Devuelve los registros de auditoria relacionados a un paciente. Solo doctores
    // o el propio paciente pueden acceder.
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<Page<AuditLogDTO>> getByPatient(Authentication authentication,
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        clinicalAccessService.requirePatientAccess(authentication.getName(), patientId);
        return ResponseEntity.ok(auditLogService.getByPatient(patientId, page, size).map(AuditLogDTO::from));
    }

    // Lista registros globales por rango de fechas. Solo accesible para doctores.
    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> getByDateRange(Authentication authentication,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireDoctor(clinicalAccessService.resolveCurrentUser(authentication.getName()));
        return ResponseEntity.ok(auditLogService.getByDateRange(start, end, page, size).map(AuditLogDTO::from));
    }

    // Verifica la integridad de la cadena de hash de auditoria. Solo accesible para doctores.
    @GetMapping("/chain/verify")
    public ResponseEntity<ChainVerificationResult> verifyChain(Authentication authentication) {
        requireDoctor(clinicalAccessService.resolveCurrentUser(authentication.getName()));
        return ResponseEntity.ok(auditLogService.verifyChain());
    }

    private void requireSelfOrDoctor(User requester, Long userId) {
        boolean isSelf = requester.getId().equals(userId);
        boolean isDoctor = requester.getRole() == RoleEnum.DOCTOR;
        if (!isSelf && !isDoctor) {
            throw new AccessDeniedException("Access denied to audit logs");
        }
    }

    private void requireDoctor(User requester) {
        if (requester.getRole() != RoleEnum.DOCTOR) {
            throw new AccessDeniedException("Only doctors can query global audit logs");
        }
    }
}
