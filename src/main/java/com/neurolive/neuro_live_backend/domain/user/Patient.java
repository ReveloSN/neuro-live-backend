package com.neurolive.neuro_live_backend.domain.user;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.data.exception.DeviceNotLinkedException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter
@NoArgsConstructor
// Modela al paciente y sus reglas de consentimiento y vinculacion.
public class Patient extends User {

    @Column(name = "consent_given", nullable = false)
    private Boolean consentGiven = false;

    @Column(name = "consent_date")
    private LocalDateTime consentDate;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLink> userLinks = new ArrayList<>();

    @Override
    protected RoleEnum supportedRole() {
        return RoleEnum.PATIENT;
    }

    public void giveConsent() {
        giveConsent(LocalDateTime.now());
    }

    public void giveConsent(LocalDateTime consentDate) {
        if (consentDate == null) {
            throw new IllegalArgumentException("Consent date is required");
        }
        this.consentGiven = true;
        this.consentDate = consentDate;
    }

    public String generateLinkToken() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase();
    }

    public void linkDevice(Long devicePatientId) {
        if (devicePatientId == null || devicePatientId <= 0) {
            throw new IllegalArgumentException("Device patient reference must be a positive identifier");
        }
        if (getId() == null) {
            throw new IllegalStateException("Patient must be persisted before linking devices");
        }
        if (!getId().equals(devicePatientId)) {
            throw new DeviceNotLinkedException("Device is not linked to the patient");
        }
    }
}
