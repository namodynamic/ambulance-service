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
        Optional<Patient> existingPatient = patientRepository.findByContact(contact);
        if (existingPatient.isPresent()) {
            return existingPatient.get();
        } else {
            Patient newPatient = new Patient(name, contact, "");
            return patientRepository.save(newPatient);
        }
    }

    @Override
    public Optional<Patient> findPatientByContact(String contact) {
        return patientRepository.findByContact(contact);
    }

    @Override
    public long countAllPatients() {
        return patientRepository.count();
    }
}