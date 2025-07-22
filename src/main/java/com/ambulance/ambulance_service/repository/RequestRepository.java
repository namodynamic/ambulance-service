package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByStatus(RequestStatus status);
    List<Request> findByRequestTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Request> findByUserContact(String userContact);
    List<Request> findAllByOrderByRequestTimeDesc();
    List<Request> findByStatusOrderByRequestTimeAsc(RequestStatus status);
}