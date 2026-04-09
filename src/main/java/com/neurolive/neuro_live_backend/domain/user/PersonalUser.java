package com.neurolive.neuro_live_backend.domain.user;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personal_users")
@Getter
@NoArgsConstructor
// Representa a un usuario personal sin rol clinico dentro de la plataforma.
public class PersonalUser extends User {

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "custom_threshold_id")
    private ActivationThreshold customThreshold;

    public PersonalUser(ActivationThreshold customThreshold) {
        this();
        setThreshold(customThreshold);
    }

    @Override
    protected RoleEnum supportedRole() {
        return RoleEnum.USER_PERSONAL;
    }

    public void setThreshold(ActivationThreshold customThreshold) {
        if (customThreshold == null) {
            throw new IllegalArgumentException("Custom threshold is required");
        }
        if (getId() != null) {
            customThreshold.assignToPersonalUser(getId(), getId());
        }
        this.customThreshold = customThreshold;
    }

    public void viewOwnDashboard() {
        if (!Boolean.TRUE.equals(getActive())) {
            throw new IllegalStateException("Inactive users cannot access the dashboard");
        }
    }
}
