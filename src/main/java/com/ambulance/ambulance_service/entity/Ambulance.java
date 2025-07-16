package com.ambulance.ambulance_service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ambulances")
public class Ambulance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_location", nullable = false)
    private String currentLocation;

    @Column(nullable = false)
    private boolean availability = true;

    // Constructors
    public Ambulance() {}

    public Ambulance(String currentLocation, boolean availability) {
        this.currentLocation = currentLocation;
        this.availability = availability;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public boolean isAvailability() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }
}