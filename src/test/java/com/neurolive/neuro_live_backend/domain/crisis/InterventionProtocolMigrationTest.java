package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:neurolive-flyway-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=update"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InterventionProtocolMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CrisisEventRepository crisisEventRepository;

    @Test
    void shouldRegisterV3MigrationAndExposeExpandedProtocolColumns() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '3' and \"success\" = true",
                Integer.class
        );
        Integer expandedColumns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_name = 'INTERVENTION_PROTOCOLS'
                  and column_name in (
                      'AUDIO_VOLUME',
                      'UI_THEME',
                      'HIGH_CONTRAST_ENABLED',
                      'BREATHING_RHYTHM',
                      'BREATHING_CYCLES'
                  )
                """,
                Integer.class
        );

        assertEquals(1, appliedMigrations);
        assertEquals(5, expandedColumns);
    }

    @Test
    void shouldPersistUiProtocolPayloadFieldsAfterMigration() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                401L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 9, 9, 0)
        );
        crisisEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.UI)
                        .uiMode("calm-focus", true)
                        .build()
        );

        CrisisEvent persistedEvent = crisisEventRepository.saveAndFlush(crisisEvent);
        Map<String, Object> persistedProtocol = jdbcTemplate.queryForMap(
                "select ui_theme, high_contrast_enabled from intervention_protocols where crisis_event_id = ?",
                persistedEvent.getId()
        );

        assertEquals("calm-focus", persistedProtocol.get("UI_THEME"));
        assertTrue(Boolean.TRUE.equals(persistedProtocol.get("HIGH_CONTRAST_ENABLED")));
    }
}
