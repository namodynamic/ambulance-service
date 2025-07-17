package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByAvailability(boolean availability);
}