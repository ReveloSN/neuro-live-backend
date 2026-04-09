package com.neurolive.neuro_live_backend.business.patterns;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InterventionNamingAndBeansTest {

    @Autowired
    private List<InterventionStrategy> interventionStrategies;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldRegisterOnlyTheRenamedInterventionBeansInPriorityOrder() {
        assertEquals(4, interventionStrategies.size());
        assertEquals(1, applicationContext.getBeanNamesForType(UIIntervention.class).length);
        assertEquals(1, applicationContext.getBeanNamesForType(BreathingIntervention.class).length);
        assertEquals(1, applicationContext.getBeanNamesForType(LightIntervention.class).length);
        assertEquals(1, applicationContext.getBeanNamesForType(AudioIntervention.class).length);
        assertInstanceOf(UIIntervention.class, interventionStrategies.get(0));
        assertInstanceOf(BreathingIntervention.class, interventionStrategies.get(1));
        assertInstanceOf(LightIntervention.class, interventionStrategies.get(2));
        assertInstanceOf(AudioIntervention.class, interventionStrategies.get(3));
        assertTrue(applicationContext.getBeansOfType(UiReductionStrategy.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(GuidedBreathingStrategy.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(LightingInterventionStrategy.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(AuditoryRegulationStrategy.class).isEmpty());
    }
}
