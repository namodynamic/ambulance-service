package com.ambulance.ambulance_service.dto;

import com.ambulance.ambulance_service.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private Role role;
    private boolean enabled;

    // Additional user details can be added here
    private String firstName;
    private String lastName;
    private String phoneNumber;

    // Timestamps
    private String createdAt;
    private String updatedAt;
}
