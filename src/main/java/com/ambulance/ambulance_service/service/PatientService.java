package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Patient;
import com.ambulance.ambulance_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService implements PatientServiceInterface<Patient> {

    private final PatientRepository patientRepository;

    @Autowired
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public <T> Optional<T> getPatientRecord(Long id, Class<T> type) {
        if (type.equals(Patient.class)) {
            return (Optional<T>) patientRepository.findById(id);
        }
        return Optional.empty();
    }

    @Override
    public <T extends Patient> T savePatientRecord(T patient) {
        return (T) patientRepository.save(patient);
    }

    @Override
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    @Override
    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    @Override
    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    @Override
    public Patient findOrCreatePatient(String name, String contact) {
        return findOrCreatePatient(name, contact, "");
    }

    public Patient findOrCreatePatient(String name, String contact, String medicalNotes) {
        List<Patient> patients = patientRepository.findByContact(contact);
        if (!patients.isEmpty()) {
            Patient existingPatient = patients.get(0);
            // Update medical notes if provided
            if (medicalNotes != null && !medicalNotes.trim().isEmpty()) {
                // Only update if the notes are different to avoid unnecessary saves
                if (!medicalNotes.equals(existingPatient.getMedicalNotes())) {
                    existingPatient.setMedicalNotes(medicalNotes);
                    return patientRepository.save(existingPatient);
                }
            }
            return existingPatient;
        } else {
            Patient newPatient = new Patient(name, contact, medicalNotes != null ? medicalNotes : "");
            return patientRepository.save(newPatient);
        }
    }

    @Override
    public Optional<Patient> findPatientByContact(String contact) {
        List<Patient> patients = patientRepository.findByContact(contact);
        return patients.isEmpty() ? Optional.empty() : Optional.of(patients.get(0));
    }

    @Override
    public long countAllPatients() {
        return patientRepository.count();
    }
}