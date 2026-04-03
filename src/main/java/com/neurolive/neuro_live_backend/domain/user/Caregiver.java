package com.neurolive.neuro_live_backend.domain.user;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "caregivers")
@Getter
@NoArgsConstructor
// Representa al cuidador autorizado para acompanar al paciente.
public class Caregiver extends User {

    @OneToMany(mappedBy = "linkedUser", cascade = CascadeType.ALL)
    private List<UserLink> userLinks = new ArrayList<>();

    @Override
    protected RoleEnum supportedRole() {
        return RoleEnum.CAREGIVER;
    }

    public void receiveAlert(String alertMessage) {
        if (alertMessage == null || alertMessage.isBlank()) {
            throw new IllegalArgumentException("Alert message is required");
        }
    }

    public void controlActuator(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Actuator command is required");
        }
    }

    public void viewPatientDashboard(Long patientId) {
        validateLinkedPatientAccess(patientId, userLinks);
    }
}
