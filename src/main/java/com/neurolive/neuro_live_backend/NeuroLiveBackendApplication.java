package com.neurolive.neuro_live_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
    }
)
public class NeuroLiveBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeuroLiveBackendApplication.class, args);
    }
}