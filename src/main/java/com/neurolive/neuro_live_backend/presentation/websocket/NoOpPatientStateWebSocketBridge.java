package com.neurolive.neuro_live_backend.presentation.websocket;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import org.springframework.stereotype.Component;

@Component
public class NoOpPatientStateWebSocketBridge implements PatientStateWebSocketBridge {

    @Override
    public void sendToCaregiverDashboard(PatientStateUpdate update) {
    }

    @Override
    public void sendToDoctorDashboard(PatientStateUpdate update) {
    }
}
