package com.ambulance.ambulance_service.dto;

import com.ambulance.ambulance_service.entity.Request;

public class RequestDTO {
    private Long id;
    private String userName;
    private String location;
    private String emergencyDescription;

    public RequestDTO(Request request) {
        this.id = request.getId();
        this.userName = request.getUserName();
        this.location = request.getLocation();
        this.emergencyDescription = request.getEmergencyDescription();
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getUserName() { return userName; }
    public String getLocation() { return location; }
    public String getEmergencyDescription() { return emergencyDescription; }
}