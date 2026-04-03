package com.neurolive.neuro_live_backend.presentation.websocket;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;

public interface PatientStateWebSocketBridge {

    void sendToCaregiverDashboard(PatientStateUpdate update);

    void sendToDoctorDashboard(PatientStateUpdate update);
}
