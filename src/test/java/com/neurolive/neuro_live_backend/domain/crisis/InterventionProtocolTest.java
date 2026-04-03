package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterventionProtocolTest {

    @Test
    void shouldCreateValidInterventionProtocol() {
        InterventionProtocol interventionProtocol = InterventionProtocol.builder(TypeEnum.LIGHTING_CONTROL)
                .light("warm-blue", 60)
                .build();

        assertEquals(TypeEnum.LIGHTING_CONTROL, interventionProtocol.getType());
        assertFalse(interventionProtocol.getActive());
        assertEquals("warm-blue", interventionProtocol.getLightColor());
        assertEquals(60, interventionProtocol.getLightIntensity());
        assertNotNull(interventionProtocol.getCreatedAt());
    }

    @Test
    void shouldRejectMissingType() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> InterventionProtocol.builder(null).build()
        );

        assertEquals("Intervention type is required", exception.getMessage());
    }

    @Test
    void shouldActivateProtocolCorrectly() {
        InterventionProtocol interventionProtocol = InterventionProtocol.builder(TypeEnum.UI_REDUCTION)
                .uiReductionEnabled()
                .build();

        interventionProtocol.activate();

        assertTrue(interventionProtocol.getActive());
    }

    @Test
    void shouldDeactivateProtocolCorrectly() {
        InterventionProtocol interventionProtocol = InterventionProtocol.builder(TypeEnum.UI_REDUCTION)
                .uiReductionEnabled()
                .active()
                .build();

        interventionProtocol.deactivate();

        assertFalse(interventionProtocol.getActive());
    }

    @Test
    void shouldValidateOptionalConfigConsistently() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> InterventionProtocol.builder(TypeEnum.AUDITORY_REGULATION)
                        .light("red", 40)
                        .build()
        );

        assertEquals("Light configuration is only allowed for lighting control protocols", exception.getMessage());
    }

    @Test
    void shouldBuildAudioProtocolCorrectly() {
        InterventionProtocol interventionProtocol = InterventionProtocol.builder(TypeEnum.AUDITORY_REGULATION)
                .audioTrack("focus-track.mp3")
                .build();

        assertEquals("focus-track.mp3", interventionProtocol.getAudioTrack());
        assertNotNull(interventionProtocol.getCreatedAt());
    }

    @Test
    void shouldRejectInvalidLightIntensity() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> InterventionProtocol.builder(TypeEnum.LIGHTING_CONTROL)
                        .light("amber", 120)
                        .build()
        );

        assertEquals("Light intensity must be between 1 and 100", exception.getMessage());
    }

    @Test
    void shouldBuildBreathingProtocolWithBuilder() {
        InterventionProtocol interventionProtocol = InterventionProtocol.builder(TypeEnum.GUIDED_BREATHING)
                .breathingEnabled()
                .active()
                .build();

        assertEquals(TypeEnum.GUIDED_BREATHING, interventionProtocol.getType());
        assertTrue(interventionProtocol.getBreathingEnabled());
        assertTrue(interventionProtocol.getActive());
    }
}
