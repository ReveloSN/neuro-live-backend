package com.neurolive.neuro_live_backend.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Verifica el comportamiento comun de la clase User: registro, perfil y desactivacion.
class UserDomainTest {

    @Test
    void register_shouldNormalizeEmailToLowercase() {
        Patient patient = new Patient();
        patient.register("Test User", "Patient@NeuroLive.TEST", "hash");

        assertEquals("patient@neurolive.test", patient.getEmail());
    }

    @Test
    void register_shouldRejectBlankName() {
        Patient patient = new Patient();
        assertThrows(IllegalArgumentException.class,
                () -> patient.register("  ", "a@b.com", "hash"));
    }

    @Test
    void register_shouldRejectBlankEmail() {
        Patient patient = new Patient();
        assertThrows(IllegalArgumentException.class,
                () -> patient.register("Name", "  ", "hash"));
    }

    @Test
    void register_shouldRejectBlankPasswordHash() {
        Patient patient = new Patient();
        assertThrows(IllegalArgumentException.class,
                () -> patient.register("Name", "a@b.com", "  "));
    }

    @Test
    void register_shouldSetUserActiveByDefault() {
        Patient patient = new Patient();
        patient.register("Patient", "p@test.com", "hash");

        assertTrue(patient.getActive());
    }

    @Test
    void deactivate_shouldSetInactive() {
        Patient patient = new Patient();
        patient.register("Patient", "p@test.com", "hash");
        patient.deactivate();

        assertFalse(patient.getActive());
    }

    @Test
    void updateProfile_shouldUpdateNameAndPhotoUrl() {
        Patient patient = new Patient();
        patient.register("Old Name", "p@test.com", "hash");
        patient.updateProfile("New Name", "https://cdn.example.com/photo.png");

        assertEquals("New Name", patient.getName());
        assertEquals("https://cdn.example.com/photo.png", patient.getPhotoUrl());
    }

    @Test
    void updateProfile_shouldRejectBlankName() {
        Patient patient = new Patient();
        patient.register("Old Name", "p@test.com", "hash");

        assertThrows(IllegalArgumentException.class,
                () -> patient.updateProfile("  ", null));
    }

    @Test
    void updateProfile_shouldNormalizeEmptyPhotoUrlToNull() {
        Patient patient = new Patient();
        patient.register("Name", "p@test.com", "hash");
        patient.updateProfile("Name", "  ");

        assertEquals(null, patient.getPhotoUrl());
    }

    @Test
    void recoverAccount_shouldUpdatePasswordHash() {
        Patient patient = new Patient();
        patient.register("Name", "p@test.com", "old-hash");
        patient.recoverAccount("new-encoded-hash");

        assertEquals("new-encoded-hash", patient.getPasswordHash());
    }
}
