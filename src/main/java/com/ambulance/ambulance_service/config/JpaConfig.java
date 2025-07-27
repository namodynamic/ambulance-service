package com.ambulance.ambulance_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.ambulance.ambulance_service")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // In a real application, you would get the current user from the security context
        // For now, we'll return a default value
        return () -> Optional.of("system");
    }
}
