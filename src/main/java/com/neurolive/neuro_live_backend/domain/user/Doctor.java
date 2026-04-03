package com.neurolive.neuro_live_backend.domain.user;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Getter
@NoArgsConstructor
// Representa al profesional de salud que puede atender pacientes vinculados.
public class Doctor extends User {

    @Column(length = 120)
    private String specialty;

    @OneToMany(mappedBy = "linkedUser", cascade = CascadeType.ALL)
    private List<UserLink> userLinks = new ArrayList<>();

    public Doctor(String specialty) {
        this();
        this.specialty = normalizeSpecialty(specialty);
    }

    @Override
    protected RoleEnum supportedRole() {
        return RoleEnum.DOCTOR;
    }

    public void setThresholds(ActivationThreshold activationThreshold) {
        if (activationThreshold == null) {
            throw new IllegalArgumentException("Activation threshold is required");
        }
    }

    public void viewClinicalAnalysis(Long patientId) {
        validateLinkedPatientAccess(patientId, userLinks);
    }

    public File exportData(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Export path is required");
        }
        return new File(path.trim());
    }

    public void updateSpecialty(String specialty) {
        this.specialty = normalizeSpecialty(specialty);
    }

    private String normalizeSpecialty(String specialty) {
        if (specialty == null) {
            return null;
        }
        String normalized = specialty.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
