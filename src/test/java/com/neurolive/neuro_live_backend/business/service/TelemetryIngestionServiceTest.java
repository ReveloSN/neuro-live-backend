package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.AudioIntervention;
import com.neurolive.neuro_live_backend.business.patterns.BreathingIntervention;
import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.business.patterns.LightIntervention;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateObserver;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.business.patterns.UIIntervention;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;
import com.neurolive.neuro_live_backend.infrastructure.config.AnalysisProperties;
import com.neurolive.neuro_live_backend.presentation.dto.TelemetryPayload;
import com.neurolive.neuro_live_backend.repository.BiometricTelemetrySampleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryIngestionServiceTest {

    @Mock
    private BiometricTelemetrySampleRepository biometricTelemetrySampleRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private BaseLineService baseLineService;

    @Mock
    private ActivationThresholdService activationThresholdService;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @Mock
    private MonitoringConsentService monitoringConsentService;

    @Mock
    private CrisisMediator crisisMediator;

    @Mock
    private CrisisOutcomePersistenceService crisisOutcomePersistenceService;

    @Mock
    private AnalysisProperties analysisProperties;

    @InjectMocks
    private TelemetryIngestionService telemetryIngestionService;

    @BeforeEach
    void configureAnalysisProperties() {
        lenient().when(analysisProperties.telemetryWindow()).thenReturn(Duration.ofMinutes(10));
    }

    @Test
    void shouldPersistValidTelemetry() {
        TelemetryPayload payload = buildPayload(81L, "AA:BB:CC:DD:EE:41", 88.0f, 97.0f, LocalDateTime.of(2026, 4, 2, 10, 0));
        Device device = buildDevice(81L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(81L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.NORMAL)
        ));

        TelemetryIngestionResult result = telemetryIngestionService.ingest(payload);

        assertEquals(storedSample, result.storedSample());
        assertEquals(device, result.updatedDevice());
        verify(biometricTelemetrySampleRepository).save(any(BiometricTelemetrySample.class));
    }

    @Test
    void shouldUpdateDeviceStatusOnValidTelemetry() {
        TelemetryPayload payload = buildPayload(82L, "AA:BB:CC:DD:EE:42", 90.0f, 97.0f, LocalDateTime.of(2026, 4, 2, 10, 5));
        Device device = buildDevice(82L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(82L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.NORMAL)
        ));

        telemetryIngestionService.ingest(payload);

        verify(deviceService).registerTelemetry(payload.deviceMac(), payload.observedAt(), null);
    }

    @Test
    void shouldCallCrisisMediatorWithBaselineThresholdContext() {
        TelemetryPayload payload = buildPayload(83L, "AA:BB:CC:DD:EE:43", 98.0f, 96.0f, LocalDateTime.of(2026, 4, 2, 10, 10));
        Device device = buildDevice(83L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(83L, 80.0f, 98.0f);
        ActivationThreshold activationThreshold = new ActivationThreshold(null, 95.0f, null, null);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(activationThreshold);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.NORMAL)
        ));

        telemetryIngestionService.ingest(payload);

        ArgumentCaptor<CrisisMediator.CrisisEvaluationInput> inputCaptor =
                ArgumentCaptor.forClass(CrisisMediator.CrisisEvaluationInput.class);
        verify(crisisMediator).mediate(inputCaptor.capture());
        assertEquals(baseLine, inputCaptor.getValue().baseLine());
        assertEquals(activationThreshold, inputCaptor.getValue().activationThreshold());
        assertEquals(payload.patientId(), inputCaptor.getValue().patientId());
        assertEquals(payload.bpm(), inputCaptor.getValue().currentBiometricData().bpm());
    }

    @Test
    void shouldElevateAssessmentWithPredictiveSignal() {
        TelemetryPayload payload = new TelemetryPayload(
                830L,
                "AA:BB:CC:DD:EE:83",
                88.0f,
                97.0f,
                LocalDateTime.of(2026, 4, 2, 10, 12),
                Boolean.TRUE,
                "PRE_CRISIS",
                0.82f,
                "Predictive trend is escalating"
        );
        Device device = buildDevice(830L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(830L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), Boolean.TRUE)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.ACTIVE_CRISIS)
        ));

        telemetryIngestionService.ingest(payload);

        ArgumentCaptor<CrisisMediator.CrisisEvaluationInput> inputCaptor =
                ArgumentCaptor.forClass(CrisisMediator.CrisisEvaluationInput.class);
        verify(crisisMediator).mediate(inputCaptor.capture());
        assertEquals(StateEnum.ACTIVE_CRISIS, inputCaptor.getValue().analysisStateHint());
    }

    @Test
    void shouldPersistPredictionFieldsWithTelemetrySample() {
        TelemetryPayload payload = new TelemetryPayload(
                831L,
                "AA:BB:CC:DD:EE:86",
                91.0f,
                96.0f,
                LocalDateTime.of(2026, 4, 2, 10, 13),
                Boolean.TRUE,
                "warning",
                1.4f,
                "x".repeat(700)
        );
        Device device = buildDevice(831L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(831L, 80.0f, 98.0f);

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), Boolean.TRUE)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(
                eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(
                BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload), "WARNING", 1.0f, "x".repeat(512))
        ));
        when(baseLineService.updateFromTelemetry(any(), any())).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.RISK_ELEVATED)
        ));

        telemetryIngestionService.ingest(payload);

        ArgumentCaptor<BiometricTelemetrySample> sampleCaptor = ArgumentCaptor.forClass(BiometricTelemetrySample.class);
        verify(biometricTelemetrySampleRepository).save(sampleCaptor.capture());
        assertEquals(Boolean.TRUE, sampleCaptor.getValue().getSensorContact());
        assertEquals("WARNING", sampleCaptor.getValue().getPredictionState());
        assertEquals(1.0f, sampleCaptor.getValue().getPredictionConfidence());
        assertEquals(512, sampleCaptor.getValue().getPredictionReasoning().length());
    }

    @Test
    void shouldStillPersistDataWhenBaselineIsNotReady() {
        TelemetryPayload payload = buildPayload(84L, "AA:BB:CC:DD:EE:44", 84.0f, 98.0f, LocalDateTime.of(2026, 4, 2, 10, 15));
        Device device = buildDevice(84L, payload.deviceMac());
        BaseLine baseLine = new BaseLine(84L);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );

        TelemetryIngestionResult result = telemetryIngestionService.ingest(payload);

        assertEquals(storedSample, result.storedSample());
        assertNull(result.crisisMediationResult());
        verify(crisisMediator, never()).mediate(any());
    }

    @Test
    void shouldNotifyCaregiverWhenSensorContactIsLost() {
        TelemetryPayload payload = new TelemetryPayload(
                840L,
                "AA:BB:CC:DD:EE:84",
                84.0f,
                98.0f,
                LocalDateTime.of(2026, 4, 2, 10, 16),
                Boolean.FALSE
        );
        Device device = buildDevice(840L, payload.deviceMac());
        Device updatedDevice = buildDevice(840L, payload.deviceMac());
        updatedDevice.recordTelemetry(payload.observedAt(), Boolean.FALSE);
        BaseLine baseLine = new BaseLine(840L);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), Boolean.FALSE)).thenReturn(updatedDevice);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );

        telemetryIngestionService.ingest(payload);

        ArgumentCaptor<PatientStateUpdate> updateCaptor = ArgumentCaptor.forClass(PatientStateUpdate.class);
        verify(crisisMediator).publishUpdate(updateCaptor.capture());
        assertTrue(updateCaptor.getValue().isSensorContactAlert());
        assertEquals(Boolean.FALSE, updateCaptor.getValue().sensorContact());
        verify(crisisMediator, never()).mediate(any());
    }

    @Test
    void shouldNotifyCaregiverWhenTelemetryReconnectsDevice() {
        TelemetryPayload payload = buildPayload(841L, "AA:BB:CC:DD:EE:85", 84.0f, 98.0f, LocalDateTime.of(2026, 4, 2, 10, 17));
        Device device = buildDevice(841L, payload.deviceMac());
        device.updateStatus(false, LocalDateTime.of(2026, 4, 2, 10, 10));
        Device updatedDevice = buildDevice(841L, payload.deviceMac());
        updatedDevice.recordTelemetry(payload.observedAt(), Boolean.TRUE);
        BaseLine baseLine = new BaseLine(841L);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(updatedDevice);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(null, null, null, null, StateEnum.NORMAL, "stable", "stable")
        );

        telemetryIngestionService.ingest(payload);

        ArgumentCaptor<PatientStateUpdate> updateCaptor = ArgumentCaptor.forClass(PatientStateUpdate.class);
        verify(crisisMediator).publishUpdate(updateCaptor.capture());
        assertEquals(Boolean.TRUE, updateCaptor.getValue().deviceConnected());
        assertEquals(Boolean.TRUE, updateCaptor.getValue().sensorContact());
        assertTrue(updateCaptor.getValue().shouldNotifyCaregiver());
        verify(crisisMediator, never()).mediate(any());
    }

    @Test
    void shouldRejectInvalidTelemetry() {
        TelemetryPayload payload = new TelemetryPayload(null, "AA:BB:CC:DD:EE:45", 80.0f, 97.0f, LocalDateTime.now());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> telemetryIngestionService.ingest(payload)
        );

        assertEquals("Patient reference must be a positive identifier", exception.getMessage());
        verify(biometricTelemetrySampleRepository, never()).save(any());
    }

    @Test
    void shouldRejectTelemetryWithoutConsentBeforePersistingSample() {
        TelemetryPayload payload = buildPayload(843L, "AA:BB:CC:DD:EE:88", 80.0f, 97.0f, LocalDateTime.now());
        doThrow(new IllegalStateException("Biometric consent is required before monitoring starts"))
                .when(monitoringConsentService)
                .assertMonitoringAllowed(843L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> telemetryIngestionService.ingest(payload)
        );

        assertEquals("Biometric consent is required before monitoring starts", exception.getMessage());
        verify(biometricTelemetrySampleRepository, never()).save(any());
        verify(deviceService, never()).findByMacAddress(any());
    }

    @Test
    void shouldRejectTelemetryWithImpossibleBiometricsAndUnknownPredictionState() {
        TelemetryPayload payload = new TelemetryPayload(
                842L,
                "AA:BB:CC:DD:EE:87",
                280.0f,
                130.0f,
                LocalDateTime.now(),
                Boolean.TRUE,
                "DANGEROUS_UNKNOWN",
                0.4f,
                "Unexpected state"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> telemetryIngestionService.ingest(payload)
        );

        assertEquals("Telemetry BPM must be between 30 and 220", exception.getMessage());
        verify(biometricTelemetrySampleRepository, never()).save(any());
    }

    @Test
    void shouldPersistCrisisOutcomeWhenMediatorDetectsCrisis() {
        TelemetryPayload payload = buildPayload(86L, "AA:BB:CC:DD:EE:47", 112.0f, 92.0f, LocalDateTime.of(2026, 4, 2, 10, 25));
        Device device = buildDevice(86L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(86L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));
        CrisisMediator.CrisisMediationResult mediationResult = buildCrisisResult(payload.patientId(), payload.observedAt());

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(0.32f, 280.0f, 330.0f, 5, StateEnum.ACTIVE_CRISIS, "error-dwell-flight", "acute")
        );
        when(crisisMediator.mediate(any())).thenReturn(mediationResult);

        telemetryIngestionService.ingest(payload);

        verify(crisisOutcomePersistenceService).persist(mediationResult);
    }

    @Test
    void shouldKeepTelemetryWhenCommandDeliveryFailsAfterCrisisPersistence() {
        TelemetryPayload payload = buildPayload(87L, "AA:BB:CC:DD:EE:48", 112.0f, 92.0f, LocalDateTime.of(2026, 4, 2, 10, 26));
        Device device = buildDevice(87L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(87L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));
        CrisisMediator.CrisisMediationResult mediationResult = buildCrisisResult(payload.patientId(), payload.observedAt());

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(riskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(0.32f, 280.0f, 330.0f, 5, StateEnum.ACTIVE_CRISIS, "error-dwell-flight", "acute")
        );
        when(crisisMediator.mediate(any())).thenReturn(mediationResult);
        doThrow(new IllegalStateException("Realtime service unavailable"))
                .when(deviceService)
                .sendCommand(any(), any(), any());

        assertDoesNotThrow(() -> telemetryIngestionService.ingest(payload));

        verify(crisisOutcomePersistenceService).persist(mediationResult);
        verify(deviceService).sendCommand(any(), any(), any());
    }

    @Test
    void shouldNotifyObserversThroughExistingMediatorFlow() {
        BiometricTelemetrySampleRepository sampleRepository = org.mockito.Mockito.mock(BiometricTelemetrySampleRepository.class);
        DeviceService localDeviceService = org.mockito.Mockito.mock(DeviceService.class);
        BaseLineService localBaseLineService = org.mockito.Mockito.mock(BaseLineService.class);
        ActivationThresholdService localThresholdService = org.mockito.Mockito.mock(ActivationThresholdService.class);
        RiskAssessmentService localRiskAssessmentService = org.mockito.Mockito.mock(RiskAssessmentService.class);
        MonitoringConsentService localMonitoringConsentService = org.mockito.Mockito.mock(MonitoringConsentService.class);
        CrisisOutcomePersistenceService localCrisisOutcomePersistenceService =
                org.mockito.Mockito.mock(CrisisOutcomePersistenceService.class);
        RecordingObserver observer = new RecordingObserver();
        CrisisMediator actualMediator = new CrisisMediator(
                List.of(
                        new UIIntervention(),
                        new BreathingIntervention(),
                        new LightIntervention(),
                        new AudioIntervention()
                ),
                List.of(observer)
        );
        AnalysisProperties localAnalysisProperties = org.mockito.Mockito.mock(AnalysisProperties.class);
        when(localAnalysisProperties.telemetryWindow()).thenReturn(Duration.ofMinutes(10));
        TelemetryIngestionService localService = new TelemetryIngestionService(
                sampleRepository,
                localDeviceService,
                localBaseLineService,
                localThresholdService,
                localRiskAssessmentService,
                localMonitoringConsentService,
                actualMediator,
                localCrisisOutcomePersistenceService,
                localAnalysisProperties
        );

        TelemetryPayload payload = buildPayload(85L, "AA:BB:CC:DD:EE:46", 96.0f, 96.0f, LocalDateTime.of(2026, 4, 2, 10, 20));
        Device device = buildDevice(85L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(85L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(localDeviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(sampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(localDeviceService.registerTelemetry(payload.deviceMac(), payload.observedAt(), null)).thenReturn(device);
        when(sampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(eq(payload.patientId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(storedSample));
        when(localBaseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(localThresholdService.resolveForPatient(payload.patientId())).thenReturn(null);
        when(localRiskAssessmentService.assess(payload.patientId(), toDomain(payload), baseLine)).thenReturn(
                new RiskAssessmentService.AssessmentSnapshot(0.12f, 170.0f, 190.0f, 1, StateEnum.RISK_ELEVATED, "error-dwell", "elevated")
        );

        TelemetryIngestionResult result = localService.ingest(payload);

        assertNotNull(result.crisisMediationResult());
        assertEquals(1, observer.updates().size());
        assertTrue(observer.updates().getFirst().emotionalState().isAtRisk());
        assertEquals(payload.patientId(), observer.updates().getFirst().patientId());
    }

    private TelemetryPayload buildPayload(Long patientId, String deviceMac, Float bpm, Float spo2, LocalDateTime observedAt) {
        return new TelemetryPayload(patientId, deviceMac, bpm, spo2, observedAt);
    }

    private Device buildDevice(Long patientId, String deviceMac) {
        Device device = new Device();
        device.register(patientId, deviceMac, null);
        setDeviceId(device, 500L + patientId);
        return device;
    }

    private BaseLine buildReadyBaseLine(Long patientId, float avgBpm, float avgSpo2) {
        BaseLine baseLine = new BaseLine(patientId);
        LocalDateTime sessionStart = LocalDateTime.of(2026, 4, 2, 9, 0);

        baseLine.calculate(List.of(
                new BiometricData(avgBpm, avgSpo2, sessionStart),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(1)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(2)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(3)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(4)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(5))
        ));

        return baseLine;
    }

    private BiometricData toDomain(TelemetryPayload payload) {
        return new BiometricData(payload.bpm(), payload.spo2(), payload.observedAt());
    }

    private CrisisMediator.CrisisMediationResult buildCrisisResult(Long patientId, LocalDateTime startedAt) {
        return CrisisMediator.CrisisMediationResult.crisisDetected(
                EmotionalState.from(StateEnum.ACTIVE_CRISIS),
                com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent.open(
                        patientId,
                        StateEnum.ACTIVE_CRISIS,
                        startedAt
                ),
                com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol.builder(
                                com.neurolive.neuro_live_backend.data.enums.TypeEnum.BREATHING
                        )
                        .breathingPattern(4, 6)
                        .build()
        );
    }

    private void setDeviceId(Device device, Long id) {
        setField(Device.class, device, "id", id);
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

    private static final class RecordingObserver implements PatientStateObserver {

        private final List<PatientStateUpdate> updates = new ArrayList<>();

        @Override
        public void onPatientStateChanged(PatientStateUpdate update) {
            updates.add(update);
        }

        private List<PatientStateUpdate> updates() {
            return updates;
        }
    }
}
