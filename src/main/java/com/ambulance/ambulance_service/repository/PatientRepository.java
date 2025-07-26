package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByContact(String contact);
    
    /**
     * Count all patients in the system
     * @return The total number of patients
     */
    long count();
}