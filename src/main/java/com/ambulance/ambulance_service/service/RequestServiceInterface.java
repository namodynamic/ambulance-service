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
    List<Request> getAllRequests();

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

    // New count methods for admin dashboard
    long countAllRequests();

    long countRequestsByStatus(String status);

    // Helper methods
    Request createRequest(Request request);

    Request updateRequest(Request request);
}
