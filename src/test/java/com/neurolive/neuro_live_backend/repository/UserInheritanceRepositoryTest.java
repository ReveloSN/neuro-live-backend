package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.NeuroLiveBackendApplication;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NeuroLiveBackendApplication.class)
@Transactional
// Prueba la persistencia de la jerarquia de usuarios en el repositorio.
class UserInheritanceRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistAndLoadSubclassInstancesThroughUserRepository() {
        Patient patient = new Patient();
        patient.register("Patient One", "patient.one@neurolive.test", "encoded-secret");

        Caregiver caregiver = new Caregiver();
        caregiver.register("Caregiver One", "caregiver.one@neurolive.test", "encoded-secret");

        userRepository.save(patient);
        userRepository.save(caregiver);

        User storedPatient = userRepository.findByEmail("patient.one@neurolive.test").orElseThrow();
        User storedCaregiver = userRepository.findByEmail("caregiver.one@neurolive.test").orElseThrow();

        assertInstanceOf(Patient.class, storedPatient);
        assertInstanceOf(Caregiver.class, storedCaregiver);
        assertEquals(RoleEnum.PATIENT, storedPatient.getRole());
        assertEquals(RoleEnum.CAREGIVER, storedCaregiver.getRole());
        assertTrue(storedPatient.getCreatedAt() != null);
    }
}
