package com.neurolive.neuro_live_backend.presentation.websocket;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DashboardObserverTest {

    @Test
    void shouldForwardCaregiverUpdatesToTheWebSocketBridge() {
        RecordingBridge recordingBridge = new RecordingBridge();
        CaregiverDashboardObserver observer = new CaregiverDashboardObserver(recordingBridge);
        PatientStateUpdate update = PatientStateUpdate.monitoring(
                71L,
                EmotionalState.from(StateEnum.RISK_ELEVATED),
                false,
                false,
                Instant.parse("2026-04-01T16:20:00Z"));

        observer.onPatientStateChanged(update);

        assertEquals(update, recordingBridge.lastCaregiverUpdate);
    }

    @Test
    void shouldForwardDoctorUpdatesToTheWebSocketBridge() {
        RecordingBridge recordingBridge = new RecordingBridge();
        DoctorDashboardObserver observer = new DoctorDashboardObserver(recordingBridge);
        PatientStateUpdate update = PatientStateUpdate.monitoring(
                72L,
                EmotionalState.from(StateEnum.ACTIVE_CRISIS),
                true,
                true,
                Instant.parse("2026-04-01T16:25:00Z"));

        observer.onPatientStateChanged(update);

        assertEquals(update, recordingBridge.lastDoctorUpdate);
    }

    @Test
    void shouldKeepCaregiverOnlyDisconnectAlertsOutOfDoctorDashboard() {
        RecordingBridge recordingBridge = new RecordingBridge();
        DoctorDashboardObserver observer = new DoctorDashboardObserver(recordingBridge);
        PatientStateUpdate update = PatientStateUpdate.caregiverDisconnectAlert(
                73L,
                Instant.parse("2026-04-01T16:30:00Z"));

        observer.onPatientStateChanged(update);

        assertNull(recordingBridge.lastDoctorUpdate);
    }

    @Test
    void shouldForwardCaregiverOnlyDisconnectAlertsToCaregiverDashboard() {
        RecordingBridge recordingBridge = new RecordingBridge();
        CaregiverDashboardObserver observer = new CaregiverDashboardObserver(recordingBridge);
        PatientStateUpdate update = PatientStateUpdate.caregiverDisconnectAlert(
                74L,
                Instant.parse("2026-04-01T16:35:00Z"));

        observer.onPatientStateChanged(update);

        assertEquals(update, recordingBridge.lastCaregiverUpdate);
    }

    private static final class RecordingBridge implements PatientStateWebSocketBridge {

        private PatientStateUpdate lastCaregiverUpdate;
        private PatientStateUpdate lastDoctorUpdate;

        @Override
        public void sendToCaregiverDashboard(PatientStateUpdate update) {
            lastCaregiverUpdate = update;
        }

        @Override
        public void sendToDoctorDashboard(PatientStateUpdate update) {
            lastDoctorUpdate = update;
        }
    }
}
