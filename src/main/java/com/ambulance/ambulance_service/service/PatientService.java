package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Patient;
import com.ambulance.ambulance_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService<T extends Patient> {

    @Autowired
    private PatientRepository patientRepository;

    // Generic method to handle patient records
    public <T> Optional<T> getPatientRecord(Long id, Class<T> type) {
        if (type.equals(Patient.class)) {
            return (Optional<T>) patientRepository.findById(id);
        }
        return Optional.empty();
    }

    // Generic method to save patient records
    public <T extends Patient> T savePatientRecord(T patient) {
        return (T) patientRepository.save(patient);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public Patient findOrCreatePatient(String name, String contact) {
        Optional<Patient> existingPatient = patientRepository.findByContact(contact);
        if (existingPatient.isPresent()) {
            return existingPatient.get();
        } else {
            Patient newPatient = new Patient(name, contact, "");
            return patientRepository.save(newPatient);
        }
    }

    public Optional<Patient> findPatientByContact(String contact) {
        return patientRepository.findByContact(contact);
    }
}