package com.ambulance.ambulance_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AmbulanceRequestDto {
    @NotNull(message = "User name is required")
    private String userName;

    @Size(max = 100, message = "Patient name must be less than 100 characters")
    private String patientName;

    @NotNull(message = "User contact is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String userContact;

    @NotNull(message = "Location is required")
    private String location;

    private String emergencyDescription;

    private String medicalNotes;

    // Constructors
    public AmbulanceRequestDto() {}

    public AmbulanceRequestDto(String userName, String patientName, String userContact, String location, String emergencyDescription, String medicalNotes) {
        this.userName = userName;
        this.patientName = patientName;
        this.userContact = userContact;
        this.location = location;
        this.emergencyDescription = emergencyDescription;
        this.medicalNotes = medicalNotes;
    }

    // Getters and Setters
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName != null ? patientName.trim() : null;
    }

    public String getUserContact() { return userContact; }
    public void setUserContact(String userContact) { this.userContact = userContact; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEmergencyDescription() { return emergencyDescription; }
    public void setEmergencyDescription(String emergencyDescription) { this.emergencyDescription = emergencyDescription; }

    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }
}