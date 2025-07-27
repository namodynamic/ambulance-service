package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RequestServiceInterface {

    // Existing methods
    Page<Request> getAllRequests(Pageable pageable);

    Optional<Request> getRequestById(Long id);

    Request createRequest(AmbulanceRequestDto requestDto, com.ambulance.ambulance_service.entity.User user)
        throws NoAvailableAmbulanceException;

    Request updateRequestStatus(Long requestId, RequestStatus status, String notes)
        throws RequestNotFoundException;

    List<Request> getRequestsByStatus(RequestStatus status);

    List<Request> getPendingRequests();

    List<Request> getRequestsByUser(User user);

    List<Request> getActiveRequestsByUser(User user);

    List<RequestStatusHistory> getRequestStatusHistory(Long requestId) throws RequestNotFoundException;

    Page<Request> getActiveUserRequests(User user, Pageable pageable);

    // Count methods for admin dashboard
    long countAllRequests();

    long countRequestsByStatus(String status);

    // CRUD operations
    
    /**
     * Creates a new request with the provided details
     * @param request the request to create
     * @return the created request
     */
    Request createRequest(Request request);

    /**
     * Updates an existing request
     * @param request the request with updated information
     * @return the updated request
     */
    Request updateRequest(Request request);
    
    /**
     * Soft deletes a request by ID
     * @param id the ID of the request to delete
     * @return true if the request was found and deleted, false otherwise
     */
    boolean deleteRequest(Long id);
    
    /**
     * Gets a request by ID, including soft-deleted ones
     * @param id the ID of the request to find
     * @return the request if found, empty otherwise
     */
    Optional<Request> findByIdIncludingDeleted(Long id);
    
    /**
     * Gets all requests, including soft-deleted ones
     * @return list of all requests
     */
    List<Request> findAllIncludingDeleted();
    
    /**
     * Gets all requests with pagination, including soft-deleted ones
     * @param pageable pagination information
     * @return page of requests
     */
    Page<Request> findAllIncludingDeleted(Pageable pageable);
    
    /**
     * Restores a soft-deleted request
     * @param id the ID of the request to restore
     * @return true if the request was found and restored, false otherwise
     */
    boolean restoreRequest(Long id);
    
    /**
     * Permanently deletes a request from the database
     * WARNING: This action cannot be undone
     * @param id the ID of the request to delete
     * @return true if the request was found and deleted, false otherwise
     */
    boolean permanentlyDeleteRequest(Long id);
}
