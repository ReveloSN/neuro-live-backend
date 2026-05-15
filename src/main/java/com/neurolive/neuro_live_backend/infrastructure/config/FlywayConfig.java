package com.neurolive.neuro_live_backend.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
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

    @Bean
    public static BeanFactoryPostProcessor flywayJpaDependencyPostProcessor() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }
            // Asegura que Hibernate valide despues de ejecutar migraciones.
            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
            entityManagerFactory.setDependsOn(appendDependency(entityManagerFactory.getDependsOn(), "flyway"));
        };
    }

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

    private static String[] appendDependency(String[] existingDependencies, String dependency) {
        if (existingDependencies == null || existingDependencies.length == 0) {
            return new String[]{dependency};
        }
        for (String existingDependency : existingDependencies) {
            if (dependency.equals(existingDependency)) {
                return existingDependencies;
            }
        }
        String[] dependencies = java.util.Arrays.copyOf(existingDependencies, existingDependencies.length + 1);
        dependencies[dependencies.length - 1] = dependency;
        return dependencies;
    }
}
