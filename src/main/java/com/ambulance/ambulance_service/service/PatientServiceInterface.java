package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientServiceInterface<T extends Patient> {

    // Existing methods
    <T> Optional<T> getPatientRecord(Long id, Class<T> type);

    <T extends Patient> T savePatientRecord(T patient);

    List<Patient> getAllPatients();

    Optional<Patient> getPatientById(Long id);

    Patient savePatient(Patient patient);

    Patient findOrCreatePatient(String name, String contact);

    Optional<Patient> findPatientByContact(String contact);

    // New count methods for admin dashboard
    long countAllPatients();

    // Other methods as needed...
}
