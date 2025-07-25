package com.ambulance.ambulance_service.entity;

public enum Role {
    USER,
    DISPATCHER,
    ADMIN;

    /**
     * Validates if the given string is a valid role
     * @param roleString the role string to validate
     * @return true if the string is a valid role, false otherwise
     */
    public static boolean isValid(String roleString) {
        if (roleString == null) {
            return false;
        }
        try {
            // Try to convert the string to a Role enum value
            Role role = Role.valueOf(roleString.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Safely converts a string to a Role enum value
     * @param roleString the role string to convert
     * @return the corresponding Role enum value, or null if invalid
     */
    public static Role fromString(String roleString) {
        if (roleString == null) {
            return null;
        }
        try {
            return Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}