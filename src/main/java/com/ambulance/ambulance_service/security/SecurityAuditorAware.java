package com.ambulance.ambulance_service.security;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Implementation of AuditorAware to get the current auditor (user) for JPA auditing.
 */
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return SecurityUtil.getCurrentUsername()
                .or(() -> Optional.of("system"));
    }
}
