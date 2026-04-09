package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.ActivationThresholdService;
import com.neurolive.neuro_live_backend.business.service.AuditLogService;
import com.neurolive.neuro_live_backend.business.service.BaseLineService;
import com.neurolive.neuro_live_backend.business.service.ClinicalAccessService;
import com.neurolive.neuro_live_backend.business.service.KeystrokeDynamicsService;
import com.neurolive.neuro_live_backend.business.service.MonitoringConsentService;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionService;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.presentation.dto.ActivationThresholdRequestDTO;
import com.neurolive.neuro_live_backend.presentation.dto.ActivationThresholdResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.BaselineResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.BiometricDataDTO;
import com.neurolive.neuro_live_backend.presentation.dto.BiometricIngestionResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.KeystrokeCaptureResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.KeystrokeDynamicsDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/biometrics")
public class BiometricController {

    private final TelemetryIngestionService telemetryIngestionService;
    private final KeystrokeDynamicsService keystrokeDynamicsService;
    private final BaseLineService baseLineService;
    private final ActivationThresholdService activationThresholdService;
    private final ClinicalAccessService clinicalAccessService;
    private final AuditLogService auditLogService;
    private final MonitoringConsentService monitoringConsentService;

    public BiometricController(TelemetryIngestionService telemetryIngestionService,
                               KeystrokeDynamicsService keystrokeDynamicsService,
                               BaseLineService baseLineService,
                               ActivationThresholdService activationThresholdService,
                               ClinicalAccessService clinicalAccessService,
                               AuditLogService auditLogService,
                               MonitoringConsentService monitoringConsentService) {
        this.telemetryIngestionService = telemetryIngestionService;
        this.keystrokeDynamicsService = keystrokeDynamicsService;
        this.baseLineService = baseLineService;
        this.activationThresholdService = activationThresholdService;
        this.clinicalAccessService = clinicalAccessService;
        this.auditLogService = auditLogService;
        this.monitoringConsentService = monitoringConsentService;
    }

    @PostMapping("/telemetry")
    public ResponseEntity<BiometricIngestionResponseDTO> ingestTelemetry(Authentication authentication,
                                                                        @Valid @RequestBody BiometricDataDTO request,
                                                                        HttpServletRequest httpServletRequest) {
        User requester = clinicalAccessService.requirePatientAccess(authentication.getName(), request.patientId());
        auditLogService.record(requester.getId(), "INGEST_TELEMETRY", request.patientId(), resolveIp(httpServletRequest));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                BiometricIngestionResponseDTO.from(telemetryIngestionService.ingest(request.toPayload()))
        );
    }

    @PostMapping("/keystrokes")
    public ResponseEntity<KeystrokeCaptureResponseDTO> captureKeystrokes(Authentication authentication,
                                                                        @Valid @RequestBody KeystrokeDynamicsDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                KeystrokeCaptureResponseDTO.from(
                        keystrokeDynamicsService.capture(
                                authentication.getName(),
                                request.userId(),
                                request.sessionId(),
                                request.dwellTime(),
                                request.flightTime(),
                                request.errorCount(),
                                request.errorRate(),
                                request.timestamp()
                        )
                )
        );
    }

    @GetMapping("/patients/{patientId}/baseline")
    public ResponseEntity<BaselineResponseDTO> getBaseline(Authentication authentication,
                                                           @PathVariable Long patientId,
                                                           HttpServletRequest httpServletRequest) {
        User requester = clinicalAccessService.requirePatientAccess(authentication.getName(), patientId);
        auditLogService.record(requester.getId(), "READ_BASELINE", patientId, resolveIp(httpServletRequest));
        return ResponseEntity.ok(BaselineResponseDTO.from(baseLineService.findByPatientId(patientId)));
    }

    @PostMapping("/patients/{patientId}/thresholds")
    public ResponseEntity<ActivationThresholdResponseDTO> createPatientThreshold(Authentication authentication,
                                                                                 @PathVariable Long patientId,
                                                                                 @Valid @RequestBody ActivationThresholdRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ActivationThresholdResponseDTO.from(
                        activationThresholdService.saveForPatient(
                                authentication.getName(),
                                patientId,
                                request.bpmMin(),
                                request.bpmMax(),
                                request.spo2Min(),
                                request.errorRateMax()
                        )
                )
        );
    }

    @PostMapping("/me/thresholds")
    public ResponseEntity<ActivationThresholdResponseDTO> createSelfThreshold(Authentication authentication,
                                                                              @Valid @RequestBody ActivationThresholdRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ActivationThresholdResponseDTO.from(
                        activationThresholdService.saveForCurrentPersonalUser(
                                authentication.getName(),
                                request.bpmMin(),
                                request.bpmMax(),
                                request.spo2Min(),
                                request.errorRateMax()
                        )
                )
        );
    }

    @PostMapping("/patients/{patientId}/consent")
    public ResponseEntity<String> grantConsent(Authentication authentication, @PathVariable Long patientId) {
        User requester = clinicalAccessService.resolveCurrentUser(authentication.getName());
        if (requester.getRole() != RoleEnum.PATIENT || !requester.getId().equals(patientId)) {
            throw new IllegalArgumentException("Only the patient can register biometric consent");
        }

        monitoringConsentService.registerPatientConsent(patientId);
        return ResponseEntity.ok("Consent registered");
    }

    private String resolveIp(HttpServletRequest httpServletRequest) {
        return httpServletRequest == null ? "unknown" : httpServletRequest.getRemoteAddr();
    }
}
