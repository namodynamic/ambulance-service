package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Patient findByRequestId(Long requestId);
}