package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.ActivationThresholdService;
import com.neurolive.neuro_live_backend.business.service.AuditLogService;
import com.neurolive.neuro_live_backend.business.service.BaseLineService;
import com.neurolive.neuro_live_backend.business.service.BiometricTelemetryQueryService;
import com.neurolive.neuro_live_backend.business.service.ClinicalAccessService;
import com.neurolive.neuro_live_backend.business.service.KeystrokeDynamicsService;
import com.neurolive.neuro_live_backend.business.service.MonitoringConsentService;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionService;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Doctor;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.mockito.Mockito.never;
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
    private final BiometricTelemetryQueryService biometricTelemetryQueryService =
            Mockito.mock(BiometricTelemetryQueryService.class);

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
                monitoringConsentService,
                biometricTelemetryQueryService
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void latestTelemetry_shouldReturnPersistedSampleForPatient() throws Exception {
        Patient patient = buildPatient(7L);
        BiometricTelemetrySample sample = buildSample();
        when(clinicalAccessService.requirePatientAccess("patient7@test.com", 7L)).thenReturn(patient);
        when(biometricTelemetryQueryService.findLatestForPatient(7L)).thenReturn(sample);

        mockMvc.perform(get("/biometrics/patients/7/telemetry/latest")
                        .principal(new TestingAuthenticationToken("patient7@test.com", "n/a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleId").value(700L))
                .andExpect(jsonPath("$.patientId").value(7L))
                .andExpect(jsonPath("$.deviceId").value("AA:BB:CC:DD:EE:FF"))
                .andExpect(jsonPath("$.bpm").value(95.0))
                .andExpect(jsonPath("$.spo2").value(98.0))
                .andExpect(jsonPath("$.sensorConnected").value(true))
                .andExpect(jsonPath("$.observedAt").value("2026-05-29T00:15:00Z"))
                .andExpect(jsonPath("$.receivedAt").value("2026-05-29T00:15:00Z"))
                .andExpect(jsonPath("$.predictionState").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.predictionConfidence").value(0.2))
                .andExpect(jsonPath("$.predictionReasoning").value("Manual direct backend test"));

        verify(auditLogService).record(7L, "READ_LATEST_TELEMETRY", 7L, "127.0.0.1");
    }

    @Test
    void shouldReturnLatestTelemetryForAuthorizedPatient() throws Exception {
        Patient patient = buildPatient(220L);
        BiometricTelemetrySample latestSample = BiometricTelemetrySample.from(
                220L,
                "AA:BB:CC:DD:EE:20",
                new BiometricData(94.0f, 97.0f, Instant.parse("2026-05-22T06:00:00Z")),
                Boolean.TRUE,
                "STABLE",
                0.61f,
                "Recent values are stable"
        );
        when(clinicalAccessService.requirePatientAccess("patient220@neurolive.test", 220L)).thenReturn(patient);
        when(biometricTelemetryQueryService.findLatestForPatient(220L)).thenReturn(latestSample);

        mockMvc.perform(get("/biometrics/patients/220/telemetry/latest")
                        .principal(new TestingAuthenticationToken("patient220@neurolive.test", "token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(220))
                .andExpect(jsonPath("$.deviceId").value("AA:BB:CC:DD:EE:20"))
                .andExpect(jsonPath("$.bpm").value(94.0))
                .andExpect(jsonPath("$.spo2").value(97.0))
                .andExpect(jsonPath("$.sensorConnected").value(true))
                .andExpect(jsonPath("$.predictionState").value("STABLE"));

        verify(auditLogService).record(220L, "READ_LATEST_TELEMETRY", 220L, "127.0.0.1");
    }

    @Test
    void latestTelemetry_shouldReturnPersistedSampleForDoctorWithActiveAccess() throws Exception {
        Doctor doctor = buildDoctor(20L);
        when(clinicalAccessService.requirePatientAccess("doctor20@test.com", 7L)).thenReturn(doctor);
        when(biometricTelemetryQueryService.findLatestForPatient(7L)).thenReturn(buildSample());

        mockMvc.perform(get("/biometrics/patients/7/telemetry/latest")
                        .principal(new TestingAuthenticationToken("doctor20@test.com", "n/a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bpm").value(95.0))
                .andExpect(jsonPath("$.spo2").value(98.0));
    }

    @Test
    void latestTelemetry_shouldReturnPersistedSampleForCaregiverWithActiveAccess() throws Exception {
        Caregiver caregiver = buildCaregiver(30L);
        when(clinicalAccessService.requirePatientAccess("caregiver30@test.com", 7L)).thenReturn(caregiver);
        when(biometricTelemetryQueryService.findLatestForPatient(7L)).thenReturn(buildSample());

        mockMvc.perform(get("/biometrics/patients/7/telemetry/latest")
                        .principal(new TestingAuthenticationToken("caregiver30@test.com", "n/a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorConnected").value(true));
    }

    @Test
    void latestTelemetry_shouldRejectUnrelatedOrRevokedAccess() throws Exception {
        when(clinicalAccessService.requirePatientAccess("doctor21@test.com", 7L))
                .thenThrow(new UnauthorizedAccessException("User is not linked to the requested patient"));

        mockMvc.perform(get("/biometrics/patients/7/telemetry/latest")
                        .principal(new TestingAuthenticationToken("doctor21@test.com", "n/a")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not linked to the requested patient"));

        verify(biometricTelemetryQueryService, never()).findLatestForPatient(7L);
    }

    @Test
    void latestTelemetry_shouldReturnNotFoundWhenPatientHasNoSamples() throws Exception {
        Patient patient = buildPatient(7L);
        when(clinicalAccessService.requirePatientAccess("patient7@test.com", 7L)).thenReturn(patient);
        when(biometricTelemetryQueryService.findLatestForPatient(7L))
                .thenThrow(new EntityNotFoundException("Latest telemetry not found for patient 7"));

        mockMvc.perform(get("/biometrics/patients/7/telemetry/latest")
                        .principal(new TestingAuthenticationToken("patient7@test.com", "n/a")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Latest telemetry not found for patient 7"));
    }

    private BiometricTelemetrySample buildSample() {
        BiometricTelemetrySample sample = BiometricTelemetrySample.from(
                7L,
                "AA:BB:CC:DD:EE:FF",
                new BiometricData(95.0f, 98.0f, Instant.parse("2026-05-29T00:15:00Z")),
                Boolean.TRUE,
                "INSUFFICIENT_DATA",
                0.2f,
                "Manual direct backend test"
        );
        setField(BiometricTelemetrySample.class, sample, "id", 700L);
        return sample;
    }

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@test.com", "hash");
        setField(User.class, patient, "id", id);
        return patient;
    }

    private Doctor buildDoctor(Long id) {
        Doctor doctor = new Doctor("Neurology");
        doctor.register("Doctor " + id, "doctor" + id + "@test.com", "hash");
        setField(User.class, doctor, "id", id);
        return doctor;
    }

    private Caregiver buildCaregiver(Long id) {
        Caregiver caregiver = new Caregiver();
        caregiver.register("Caregiver " + id, "caregiver" + id + "@test.com", "hash");
        setField(User.class, caregiver, "id", id);
        return caregiver;
    }

    private void setField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set test field " + fieldName, exception);
        }
    }
}
