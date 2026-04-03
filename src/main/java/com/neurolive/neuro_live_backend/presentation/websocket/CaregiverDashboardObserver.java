package com.neurolive.neuro_live_backend.presentation.websocket;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateObserver;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import org.springframework.stereotype.Component;

@Component
public class CaregiverDashboardObserver implements PatientStateObserver {

    private final PatientStateWebSocketBridge patientStateWebSocketBridge;

    public CaregiverDashboardObserver(PatientStateWebSocketBridge patientStateWebSocketBridge) {
        this.patientStateWebSocketBridge = patientStateWebSocketBridge;
    }

    @Override
    // Evita acoplar el mediador con cada dashboard concreto
    public void onPatientStateChanged(PatientStateUpdate update) {
        if (update.shouldNotifyCaregiver()) {
            patientStateWebSocketBridge.sendToCaregiverDashboard(update);
        }
    }
}
