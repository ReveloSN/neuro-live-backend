package com.neurolive.neuro_live_backend.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(value = "spring.flyway.enabled", havingValue = "true")
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(DataSource dataSource,
                         @Value("${spring.flyway.locations:classpath:db/migration}") String locations,
                         @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations.split("\\s*,\\s*"))
                .baselineOnMigrate(baselineOnMigrate)
                .load();
    }
}
