package com.neurolive.neuro_live_backend.presentation.websocket;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateObserver;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import org.springframework.stereotype.Component;

@Component
public class DoctorDashboardObserver implements PatientStateObserver {

    private final PatientStateWebSocketBridge patientStateWebSocketBridge;

    public DoctorDashboardObserver(PatientStateWebSocketBridge patientStateWebSocketBridge) {
        this.patientStateWebSocketBridge = patientStateWebSocketBridge;
    }

    @Override
    public void onPatientStateChanged(PatientStateUpdate update) {
        if (update.shouldNotifyDoctor()) {
            patientStateWebSocketBridge.sendToDoctorDashboard(update);
        }
    }
}
