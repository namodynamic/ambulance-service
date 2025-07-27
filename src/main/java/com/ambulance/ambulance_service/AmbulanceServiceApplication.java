package com.ambulance.ambulance_service;

import com.ambulance.ambulance_service.security.SecurityAuditorAware;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class AmbulanceServiceApplication {

    public static void main(String[] args) {
        // Load .env file before Spring starts
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().load();
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        SpringApplication.run(AmbulanceServiceApplication.class, args);
    }

    @Bean
    public AuditorAware<String> securityAuditorAware() {
        return new SecurityAuditorAware();
    }
}
