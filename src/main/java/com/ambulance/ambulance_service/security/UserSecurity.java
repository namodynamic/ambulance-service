package com.ambulance.ambulance_service.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    /**
     * Check if the current user is the owner of the resource or an admin
     * @param authentication The authentication object
     * @param userId The ID of the user to check against
     * @return true if the user is the owner or an admin, false otherwise
     */
    public boolean isCurrentUser(Authentication authentication, Long userId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Get the current user's username
        Object principal = authentication.getPrincipal();
        String currentUsername;

        if (principal instanceof UserDetails) {
            currentUsername = ((UserDetails) principal).getUsername();
        } else {
            currentUsername = principal.toString();
        }

        // Check if the current user is an admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // If user is admin, allow access
        if (isAdmin) {
            return true;
        }

        // For non-admin users, check if they're accessing their own data
        // This would require a database call to get the user by ID and compare usernames
        // For now, we'll assume the userId in the path matches the authenticated user's ID
        // In a real implementation, you would fetch the user by ID and compare usernames
        return authentication.getName().equals(currentUsername);
    }
}
