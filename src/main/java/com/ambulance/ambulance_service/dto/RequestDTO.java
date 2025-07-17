package com.ambulance.ambulance_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class RequestDTO {
    @NotNull(message = "User name is required")
    private String userName;
    
    @NotNull(message = "Contact is required")
    @Pattern(regexp = "\\d{10}", message = "Contact must be 10 digits")
    private String userContact;
    
    @NotNull(message = "Location is required")
    private String location;

    // Getters and Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserContact() {
        return userContact;
    }

    public void setUserContact(String userContact) {
        this.userContact = userContact;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}