package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    List<Patient> findByContact(String contact);
    
    // Find non-deleted patient by ID
    @Query("SELECT p FROM Patient p WHERE p.id = :id AND p.deleted = false")
    Optional<Patient> findByIdAndDeletedFalse(@Param("id") Long id);
    
    // Find all non-deleted patients
    @Query("SELECT p FROM Patient p WHERE p.deleted = false")
    List<Patient> findByDeletedFalse();
    
    // Find non-deleted patient by contact
    @Query("SELECT p FROM Patient p WHERE p.contact = :contact AND p.deleted = false")
    Optional<Patient> findByContactAndDeletedFalse(@Param("contact") String contact);
    
    // Count non-deleted patients
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.deleted = false")
    long countByDeletedFalse();
    
    // Count all patients (including deleted ones)
    @Override
    @Query("SELECT COUNT(p) FROM Patient p")
    long count();
}