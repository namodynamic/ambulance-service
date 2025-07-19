package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.ServiceHistory;
import com.ambulance.ambulance_service.entity.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceHistoryRepository extends JpaRepository<ServiceHistory, Long> {
    List<ServiceHistory> findByStatus(ServiceStatus status);
    List<ServiceHistory> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT sh FROM ServiceHistory sh JOIN sh.request r WHERE r.userContact = :contact")
    List<ServiceHistory> findByPatientContact(String contact);
}