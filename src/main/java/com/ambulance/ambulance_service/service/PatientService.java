package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Patient;
import com.ambulance.ambulance_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Optional<Patient> updatePatient(Long id, Patient patientDetails) {
        return patientRepository.findById(id)
                .map(existingPatient -> {
                    // Only update non-null fields from patientDetails
                    if (patientDetails.getName() != null) {
                        existingPatient.setName(patientDetails.getName());
                    }

                    if (patientDetails.getContact() != null) {
                        existingPatient.setContact(patientDetails.getContact());
                    }

                    if (patientDetails.getMedicalNotes() != null) {
                        // If medical notes are being updated, we'll replace them completely
                        existingPatient.setMedicalNotes(patientDetails.getMedicalNotes());
                    }

                    // The updatedAt timestamp will be automatically updated by @LastModifiedDate
                    return patientRepository.save(existingPatient);
                });
    }

    @Override
    public <T> Optional<T> getPatientRecord(Long id, Class<T> type) {
        if (type.equals(Patient.class)) {
            return (Optional<T>) patientRepository.findByIdAndDeletedFalse(id);
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public <T extends Patient> T savePatientRecord(T patient) {
        return (T) patientRepository.save(patient);
    }

    @Override
    public List<Patient> getAllPatients() {
        return patientRepository.findByDeletedFalse();
    }

    @Override
    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    @Transactional
    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    @Override
    public Patient findOrCreatePatient(String name, String contact) {
        return findOrCreatePatient(name, contact, "");
    }

    public Patient findOrCreatePatient(String name, String contact, String medicalNotes) {
        return patientRepository.findByContactAndDeletedFalse(contact)
                .map(existingPatient -> {
                    // Update medical notes if provided
                    if (medicalNotes != null && !medicalNotes.trim().isEmpty()) {
                        // Only update if the notes are different to avoid unnecessary saves
                        if (!medicalNotes.equals(existingPatient.getMedicalNotes())) {
                            existingPatient.setMedicalNotes(medicalNotes);
                            return patientRepository.save(existingPatient);
                        }
                    }
                    return existingPatient;
                })
                .orElseGet(() -> {
                    Patient newPatient = new Patient(name, contact, medicalNotes != null ? medicalNotes : "");
                    return patientRepository.save(newPatient);
                });
    }

    @Override
    public Optional<Patient> findPatientByContact(String contact) {
        return patientRepository.findByContactAndDeletedFalse(contact);
    }

    @Override
    public long countAllPatients() {
        return patientRepository.countByDeletedFalse();
    }

    @Override
    @Transactional
    public boolean deletePatient(Long id) {
        return patientRepository.findById(id)
                .map(patient -> {
                    if (patient.isDeleted()) {
                        return false; // Already deleted
                    }
                    patient.setDeleted(true);
                    patientRepository.save(patient);
                    return true;
                })
                .orElse(false);
    }
}