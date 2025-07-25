package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;

import java.util.List;
import java.util.Optional;

public interface AmbulanceServiceInterface {

    // Existing methods
    List<Ambulance> getAllAmbulances();

    Optional<Ambulance> getAmbulanceById(Long id);

    Ambulance saveAmbulance(Ambulance ambulance);

    void updateAmbulanceStatus(Long ambulanceId, AvailabilityStatus status);

    List<Ambulance> getAvailableAmbulances();

    Optional<Ambulance> getNextAvailableAmbulance();

    // New count methods for admin dashboard
    long countAllAmbulances();

    long countAmbulancesByStatus(AvailabilityStatus status);

    // Other methods as needed...
}
