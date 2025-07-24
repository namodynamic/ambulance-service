package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.entity.RequestStatus;
import com.ambulance.ambulance_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByStatus(RequestStatus status);
    List<Request> findByRequestTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Request> findByUserContact(String userContact);
    List<Request> findAllByOrderByRequestTimeDesc();
    List<Request> findByStatusOrderByRequestTimeAsc(RequestStatus status);
    
    /**
     * Find all requests where status is not in the given list
     * @param statuses List of statuses to exclude
     * @return List of requests with status not in the provided list
     */
    List<Request> findByStatusNotIn(List<RequestStatus> statuses);
    
    /**
     * Find all requests where status is in the given list, with pagination
     * @param statuses List of statuses to include
     * @param pageable Pagination information
     * @return Page of requests with status in the provided list
     */
    Page<Request> findByStatusIn(List<RequestStatus> statuses, Pageable pageable);
    
    // Find requests by user (authenticated users)
    Page<Request> findByUser(User user, Pageable pageable);
    
    // Find requests by user and status list (for active/history views)
    Page<Request> findByUserAndStatusIn(User user, List<RequestStatus> statuses, Pageable pageable);
    
    // Existing methods for backward compatibility with contact-based queries
    Page<Request> findByUserContact(String userContact, Pageable pageable);
    Page<Request> findByUserContactAndStatusIn(String userContact, List<RequestStatus> statuses, Pageable pageable);
    
    // Find by ID and user (for security)
    Optional<Request> findByIdAndUser(Long id, User user);

    @Query("SELECT r FROM Request r ORDER BY r.requestTime DESC")
    Page<Request> findAllOrderByRequestTimeDesc(Pageable pageable);

    @Query("SELECT r FROM Request r WHERE r.status = :status ORDER BY r.requestTime ASC")
    Page<Request> findByStatusOrderByRequestTimeAsc(
            @Param("status") RequestStatus status,
            Pageable pageable
    );
}