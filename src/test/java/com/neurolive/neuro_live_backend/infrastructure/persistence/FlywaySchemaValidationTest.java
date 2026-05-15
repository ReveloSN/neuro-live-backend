package com.neurolive.neuro_live_backend.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Verifica que Flyway cree el esquema completo usado por JPA.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway-validation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "gemini.enabled=false",
        "management.health.mail.enabled=false"
})
class FlywaySchemaValidationTest {

    @Test
    void shouldStartWithFlywayAndHibernateValidation() {
        // Si el contexto carga, Flyway y Hibernate validate son compatibles.
    }
}
