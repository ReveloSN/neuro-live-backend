package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.ActivationThresholdService;
import com.neurolive.neuro_live_backend.business.service.AuditLogService;
import com.neurolive.neuro_live_backend.business.service.BaseLineService;
import com.neurolive.neuro_live_backend.business.service.ClinicalAccessService;
import com.neurolive.neuro_live_backend.business.service.KeystrokeDynamicsService;
import com.neurolive.neuro_live_backend.business.service.MonitoringConsentService;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionService;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verifica que el frontend tenga un endpoint REST estable para leer telemetria real.
class BiometricControllerTest {

    private final TelemetryIngestionService telemetryIngestionService = Mockito.mock(TelemetryIngestionService.class);
    private final KeystrokeDynamicsService keystrokeDynamicsService = Mockito.mock(KeystrokeDynamicsService.class);
    private final BaseLineService baseLineService = Mockito.mock(BaseLineService.class);
    private final ActivationThresholdService activationThresholdService = Mockito.mock(ActivationThresholdService.class);
    private final ClinicalAccessService clinicalAccessService = Mockito.mock(ClinicalAccessService.class);
    private final AuditLogService auditLogService = Mockito.mock(AuditLogService.class);
    private final MonitoringConsentService monitoringConsentService = Mockito.mock(MonitoringConsentService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BiometricController controller = new BiometricController(
                telemetryIngestionService,
                keystrokeDynamicsService,
                baseLineService,
                activationThresholdService,
                clinicalAccessService,
                auditLogService,
                monitoringConsentService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller, new ApiExceptionHandler()).build();
    }

    @Test
    void shouldReturnLatestTelemetryForAuthorizedPatient() throws Exception {
        User requester = Mockito.mock(User.class);
        when(requester.getId()).thenReturn(220L);
        BiometricTelemetrySample latestSample = BiometricTelemetrySample.from(
                220L,
                "AA:BB:CC:DD:EE:20",
                new BiometricData(94.0f, 97.0f, LocalDateTime.of(2026, 5, 22, 6, 0)),
                "STABLE",
                0.61f,
                "Recent values are stable"
        );
        when(clinicalAccessService.requirePatientAccess("patient220@neurolive.test", 220L)).thenReturn(requester);
        when(telemetryIngestionService.findLatestForPatient(220L)).thenReturn(latestSample);

        mockMvc.perform(get("/biometrics/patients/220/telemetry/latest")
                        .principal(new UsernamePasswordAuthenticationToken("patient220@neurolive.test", "token"))
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(220))
                .andExpect(jsonPath("$.deviceMac").value("AA:BB:CC:DD:EE:20"))
                .andExpect(jsonPath("$.bpm").value(94.0))
                .andExpect(jsonPath("$.spo2").value(97.0))
                .andExpect(jsonPath("$.predictionState").value("STABLE"));

        verify(auditLogService).record(220L, "READ_LATEST_TELEMETRY", 220L, "127.0.0.1");
    }
}
