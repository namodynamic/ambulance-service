package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;

import java.util.List;
import java.util.Optional;

public interface AmbulanceServiceInterface {

    List<Ambulance> getAllAmbulances();

    Optional<Ambulance> getAmbulanceById(Long id);

    Ambulance saveAmbulance(Ambulance ambulance);

    void updateAmbulanceStatus(Long ambulanceId, AvailabilityStatus status);

    List<Ambulance> getAvailableAmbulances();

    Optional<Ambulance> getNextAvailableAmbulance();

    // Count methods for admin dashboard
    long countAllAmbulances();

    long countAmbulancesByStatus(AvailabilityStatus status);
    
    /**
     * Soft deletes an ambulance by ID
     * @param id the ID of the ambulance to delete
     * @return true if the ambulance was found and deleted, false otherwise
     */
    boolean deleteAmbulance(Long id);
    
    /**
     * Updates an existing ambulance with new information
     * @param id the ID of the ambulance to update
     * @param ambulanceDetails the updated ambulance details
     * @return the updated ambulance, or empty if the ambulance was not found
     */
    Optional<Ambulance> updateAmbulance(Long id, Ambulance ambulanceDetails);
    
    /**
     * Creates a new ambulance with the provided details
     * @param ambulance the ambulance details to create
     * @return the created ambulance
     */
    Ambulance createAmbulance(Ambulance ambulance);
    
    /**
     * Finds an ambulance by license plate, including deleted ones
     * @param licensePlate the license plate to search for
     * @return the ambulance if found, empty otherwise
     */
    Optional<Ambulance> findByLicensePlateIncludingDeleted(String licensePlate);
}
