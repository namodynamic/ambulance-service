package com.ambulance.ambulance_service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        // Test user for authentication in tests
        return new InMemoryUserDetailsManager(
            User.withUsername("testuser")
                .password("{noop}password")
                .roles("USER")
                .build()
        );
    }

}
