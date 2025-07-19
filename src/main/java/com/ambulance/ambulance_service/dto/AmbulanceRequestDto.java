package com.ambulance.ambulance_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AmbulanceRequestDto {
    @NotNull(message = "User name is required")
    private String userName;

    @NotNull(message = "User contact is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String userContact;

    @NotNull(message = "Location is required")
    private String location;

    private String emergencyDescription;

    // Constructors
    public AmbulanceRequestDto() {}

    public AmbulanceRequestDto(String userName, String userContact, String location, String emergencyDescription) {
        this.userName = userName;
        this.userContact = userContact;
        this.location = location;
        this.emergencyDescription = emergencyDescription;
    }

    // Getters and Setters
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserContact() { return userContact; }
    public void setUserContact(String userContact) { this.userContact = userContact; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEmergencyDescription() { return emergencyDescription; }
    public void setEmergencyDescription(String emergencyDescription) { this.emergencyDescription = emergencyDescription; }
}