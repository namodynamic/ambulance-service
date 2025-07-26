package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.ServiceHistory;
import com.ambulance.ambulance_service.entity.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceHistoryRepository extends JpaRepository<ServiceHistory, Long> {
    List<ServiceHistory> findByStatus(ServiceStatus status);
    List<ServiceHistory> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT sh FROM ServiceHistory sh JOIN sh.request r WHERE r.userContact = :contact")
    List<ServiceHistory> findByPatientContact(String contact);
    List<ServiceHistory> findByRequestId(Long requestId);

    @Query("SELECT sh FROM ServiceHistory sh WHERE sh.request.id = :requestId ORDER BY sh.createdAt DESC")
    Optional<ServiceHistory> findFirstByRequestIdOrderByCreatedAtDesc(@Param("requestId") Long requestId);
}