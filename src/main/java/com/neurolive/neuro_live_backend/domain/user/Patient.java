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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@NoArgsConstructor
// Modela al paciente y sus reglas de consentimiento y vinculacion.
public class Patient extends User {

    private static final String LINK_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LINK_CODE_LENGTH = 6;
    private static final SecureRandom LINK_CODE_RANDOM = new SecureRandom();

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
        // Genera un codigo corto para vinculacion sin afectar tokens de seguridad.
        StringBuilder code = new StringBuilder(LINK_CODE_LENGTH);
        for (int index = 0; index < LINK_CODE_LENGTH; index++) {
            code.append(LINK_CODE_ALPHABET.charAt(LINK_CODE_RANDOM.nextInt(LINK_CODE_ALPHABET.length())));
        }
        return code.toString();
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
