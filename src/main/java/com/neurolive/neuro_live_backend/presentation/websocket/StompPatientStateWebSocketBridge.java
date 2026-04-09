package com.neurolive.neuro_live_backend.presentation.websocket;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.presentation.dto.PatientStateMessageDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
// Publica estados clinicos y alertas a topics STOMP por paciente.
public class StompPatientStateWebSocketBridge implements PatientStateWebSocketBridge {

    private static final String CAREGIVER_TOPIC = "/topic/patients/%s/caregiver";
    private static final String DOCTOR_TOPIC = "/topic/patients/%s/doctor";

    private final SimpMessagingTemplate messagingTemplate;

    public StompPatientStateWebSocketBridge(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendToCaregiverDashboard(PatientStateUpdate update) {
        messagingTemplate.convertAndSend(
                CAREGIVER_TOPIC.formatted(update.patientId()),
                PatientStateMessageDTO.from(update)
        );
    }

    @Override
    public void sendToDoctorDashboard(PatientStateUpdate update) {
        messagingTemplate.convertAndSend(
                DOCTOR_TOPIC.formatted(update.patientId()),
                PatientStateMessageDTO.from(update)
        );
    }
}
