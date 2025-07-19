package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByAvailability(AvailabilityStatus status);

    @Query("SELECT a FROM Ambulance a WHERE a.availability = 'AVAILABLE' ORDER BY a.id LIMIT 1")
    Optional<Ambulance> findFirstAvailableAmbulance();
}