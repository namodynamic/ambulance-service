package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.entity.RequestStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestStatusHistoryRepository extends JpaRepository<RequestStatusHistory, Long> {

    /**
     * Find all status history entries for a specific request, ordered by creation time in descending order
     * @param request The request to get history for
     * @return List of status history entries, most recent first
     */
    List<RequestStatusHistory> findByRequestOrderByCreatedAtDesc(Request request);

    /**
     * Find all status history entries for a specific request ID, ordered by creation time in descending order
     * @param requestId The ID of the request to get history for
     * @return List of status history entries, most recent first
     */
    List<RequestStatusHistory> findByRequestIdOrderByCreatedAtDesc(Long requestId);
}
