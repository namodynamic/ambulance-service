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
        // Only load .env file in development
        if (System.getenv("SPRING_PROFILES_ACTIVE") == null ||
                !System.getenv("SPRING_PROFILES_ACTIVE").equals("production")) {
            try {
                io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().load();
                dotenv.entries().forEach(e -> {
                    if (System.getProperty(e.getKey()) == null) {
                        System.setProperty(e.getKey(), e.getValue());
                    }
                });
            } catch (Exception e) {
                System.out.println("No .env file found, using system environment variables");
            }
        }

        SpringApplication.run(AmbulanceServiceApplication.class, args);
    }


    @Bean
    public AuditorAware<String> securityAuditorAware() {
        return new SecurityAuditorAware();
    }
}
