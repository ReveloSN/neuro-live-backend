package com.neurolive.neuro_live_backend.domain.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:neurolive-user-link-flyway-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=update"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserLinkMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRegisterV4MigrationAndExposeTokenLifecycleColumns() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '4' and \"success\" = true",
                Integer.class
        );
        Integer expandedColumns = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_name = 'USER_LINKS'
                  and column_name in ('EXPIRES_AT', 'CONSUMED_AT')
                """,
                Integer.class
        );

        assertEquals(1, appliedMigrations);
        assertEquals(2, expandedColumns);
    }

    @Test
    void shouldAllowPendingLinksWithoutTargetUserUntilTokenRedemption() {
        String linkedUserNullable = jdbcTemplate.queryForObject(
                """
                select is_nullable
                from information_schema.columns
                where table_name = 'USER_LINKS'
                  and column_name = 'LINKED_USER_ID'
                """,
                String.class
        );
        String linkTypeNullable = jdbcTemplate.queryForObject(
                """
                select is_nullable
                from information_schema.columns
                where table_name = 'USER_LINKS'
                  and column_name = 'LINK_TYPE'
                """,
                String.class
        );

        assertEquals("YES", linkedUserNullable);
        assertEquals("YES", linkTypeNullable);
    }
}
