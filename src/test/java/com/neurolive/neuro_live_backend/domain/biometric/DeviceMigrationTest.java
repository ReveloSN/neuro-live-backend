package com.neurolive.neuro_live_backend.domain.biometric;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:neurolive-device-flyway-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=update"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// Verifica que la migracion de dispositivos deje columnas utiles para RF05 y RF07.
class DeviceMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRegisterV5MigrationAndExposeConnectivityColumns() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '5' and \"success\" = true",
                Integer.class
        );
        Integer expandedColumns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_name = 'DEVICES'
                  and column_name in ('LINKED_AT', 'SENSOR_CONTACT')
                """,
                Integer.class
        );

        assertEquals(1, appliedMigrations);
        assertEquals(2, expandedColumns);
    }
}
