package com.neurolive.neuro_live_backend.presentation.websocket;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
                LocalDateTime.of(2026, 4, 1, 16, 20));

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
                LocalDateTime.of(2026, 4, 1, 16, 25));

        observer.onPatientStateChanged(update);

        assertEquals(update, recordingBridge.lastDoctorUpdate);
    }

    @Test
    void shouldKeepCaregiverOnlyDisconnectAlertsOutOfDoctorDashboard() {
        RecordingBridge recordingBridge = new RecordingBridge();
        DoctorDashboardObserver observer = new DoctorDashboardObserver(recordingBridge);
        PatientStateUpdate update = PatientStateUpdate.caregiverDisconnectAlert(
                73L,
                LocalDateTime.of(2026, 4, 1, 16, 30));

        observer.onPatientStateChanged(update);

        assertNull(recordingBridge.lastDoctorUpdate);
    }

    @Test
    void shouldForwardCaregiverOnlyDisconnectAlertsToCaregiverDashboard() {
        RecordingBridge recordingBridge = new RecordingBridge();
        CaregiverDashboardObserver observer = new CaregiverDashboardObserver(recordingBridge);
        PatientStateUpdate update = PatientStateUpdate.caregiverDisconnectAlert(
                74L,
                LocalDateTime.of(2026, 4, 1, 16, 35));

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
