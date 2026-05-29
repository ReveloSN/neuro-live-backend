package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.UserLinkService;
import com.neurolive.neuro_live_backend.data.enums.LinkTypeEnum;
import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.domain.user.UserLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verifica el endpoint seguro para revocar vinculos sin borrar historia clinica.
class UserLinkControllerTest {

    private final UserLinkService userLinkService = Mockito.mock(UserLinkService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserLinkController(userLinkService), new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldRevokeLinkForAuthenticatedParticipant() throws Exception {
        Patient patient = buildPatient(301L);
        Caregiver caregiver = buildCaregiver(401L);
        UserLink userLink = new UserLink(patient, caregiver, LinkTypeEnum.CAREGIVER);
        setField(userLink, UserLink.class, "id", 901L);
        userLink.generateToken(LocalDateTime.now().plusMinutes(15));
        userLink.activate();
        userLink.revoke(LocalDateTime.of(2026, 5, 22, 7, 10));

        when(userLinkService.revokeForRequester("patient301@neurolive.test", 901L, "127.0.0.1"))
                .thenReturn(userLink);

        mockMvc.perform(patch("/links/901/revoke")
                        .principal(new UsernamePasswordAuthenticationToken("patient301@neurolive.test", "token"))
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkId").value(901))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokedAt").exists())
                .andExpect(jsonPath("$.message").value("Link revoked successfully"));

        verify(userLinkService).revokeForRequester("patient301@neurolive.test", 901L, "127.0.0.1");
    }

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@neurolive.test", "encoded-secret");
        setId(patient, id);
        return patient;
    }

    private Caregiver buildCaregiver(Long id) {
        Caregiver caregiver = new Caregiver();
        caregiver.register("Caregiver " + id, "caregiver" + id + "@neurolive.test", "encoded-secret");
        setId(caregiver, id);
        return caregiver;
    }

    private void setId(User user, Long id) {
        setField(user, User.class, "id", id);
    }

    private void setField(Object target, Class<?> owner, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to prepare test fixture", exception);
        }
    }
}
