package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientServiceInterface<T extends Patient> {

    <T> Optional<T> getPatientRecord(Long id, Class<T> type);

    <T extends Patient> T savePatientRecord(T patient);

    List<Patient> getAllPatients();

    Optional<Patient> getPatientById(Long id);

    Patient savePatient(Patient patient);

    Patient findOrCreatePatient(String name, String contact);

    Optional<Patient> findPatientByContact(String contact);

    // New count methods for admin dashboard
    long countAllPatients();

    /**
     * Soft deletes a patient by ID
     * @param id the ID of the patient to delete
     * @return true if the patient was found and deleted, false otherwise
     */
    boolean deletePatient(Long id);
    
    /**
     * Updates an existing patient with new information
     * @param id the ID of the patient to update
     * @param patientDetails the updated patient details
     * @return the updated patient, or empty if the patient was not found
     */
    Optional<Patient> updatePatient(Long id, Patient patientDetails);

    // Other methods as needed...
}
