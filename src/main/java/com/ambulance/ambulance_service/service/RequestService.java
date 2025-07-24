package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import com.ambulance.ambulance_service.repository.RequestStatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class RequestService {
    private static final int MAX_RETRIES = 3;

    private static final Logger logger = LoggerFactory.getLogger(RequestService.class);

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private AmbulanceService ambulanceService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ServiceHistoryService serviceHistoryService;

    @Autowired
    private RequestStatusHistoryRepository statusHistoryRepository;

    public List<Request> getAllRequests() {
        return requestRepository.findAllByOrderByRequestTimeDesc();
    }

    public Optional<Request> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    @Transactional
    public Request createRequest(AmbulanceRequestDto requestDto, com.ambulance.ambulance_service.entity.User user)
            throws NoAvailableAmbulanceException {
        
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return createRequestTransaction(requestDto, user);
            } catch (ObjectOptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    logger.warn("Max retries ({}) reached for ambulance assignment", MAX_RETRIES);
                    break;
                }
                logger.info("Retrying ambulance assignment (attempt {}/{})", attempt, MAX_RETRIES);
            }
        }
        
        // If we get here, all retries failed - queue the request
        return queueRequest(requestDto, user);
    }

    private Request createRequestTransaction(AmbulanceRequestDto requestDto, com.ambulance.ambulance_service.entity.User user) throws NoAvailableAmbulanceException {
        Request request = new Request(
                requestDto.getUserName(),
                requestDto.getUserContact(),
                requestDto.getLocation(),
                requestDto.getEmergencyDescription()
        );

        // Set the user if provided (for authenticated users)
        if (user != null) {
            request.setUser(user);
        }

        // Save request first
        request = requestRepository.save(request);

        // Try to assign an ambulance
        Optional<Ambulance> availableAmbulance = ambulanceService.getNextAvailableAmbulance();
        if (availableAmbulance.isPresent()) {
            Ambulance ambulance = availableAmbulance.get();
            request.setAmbulance(ambulance);
            request.setStatus(RequestStatus.DISPATCHED);
            request.setDispatchTime(LocalDateTime.now());

            // Create or find patient
            Patient patient = patientService.findOrCreatePatient(
                    requestDto.getUserName(),
                    requestDto.getUserContact()
            );

            // Create service history
            serviceHistoryService.createServiceHistory(request, patient, ambulance);

            return requestRepository.save(request);
        } else {
            throw new NoAvailableAmbulanceException("No ambulance available at the moment");
        }
    }

    private Request queueRequest(AmbulanceRequestDto requestDto, com.ambulance.ambulance_service.entity.User user) {
        logger.info("No ambulances available - adding request to queue");
        
        // Create and save the request with PENDING status
        Request request = new Request(
                requestDto.getUserName(),
                requestDto.getUserContact(),
                requestDto.getLocation(),
                requestDto.getEmergencyDescription()
        );
        // Set the user if provided (for authenticated users)
        if (user != null) {
            request.setUser(user);
        }

        request.setStatus(RequestStatus.PENDING);
        
        // Save the request (without assigning an ambulance)
        request = requestRepository.save(request);
        
        // Log the queued request
        logger.info("Request {} added to queue", request.getId());
        
        return request;
    }

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    @Transactional
    public void processQueuedRequests() {
        List<Request> queuedRequests = requestRepository.findByStatusOrderByRequestTimeAsc(RequestStatus.PENDING);
        
        if (queuedRequests.isEmpty()) {
            return;
        }
        
        logger.info("Processing {} queued requests", queuedRequests.size());
        
        for (Request request : queuedRequests) {
            try {
                Optional<Ambulance> availableAmbulance = ambulanceService.getNextAvailableAmbulance();
                if (availableAmbulance.isPresent()) {
                    // Assign the ambulance and update status
                    Ambulance ambulance = availableAmbulance.get();
                    request.setAmbulance(ambulance);
                    request.setStatus(RequestStatus.DISPATCHED);
                    request.setDispatchTime(LocalDateTime.now());
                    
                    // Create or find patient
                    Patient patient = patientService.findOrCreatePatient(
                            request.getUserName(),
                            request.getUserContact()
                    );
                    
                    // Create service history
                    serviceHistoryService.createServiceHistory(request, patient, ambulance);
                    
                    requestRepository.save(request);
                    
                    logger.info("Assigned ambulance {} to request {}", ambulance.getId(), request.getId());
                } else {
                    logger.info("No ambulances available for request {}", request.getId());
                    break; // No more ambulances available, try again later
                }
            } catch (Exception e) {
                logger.error("Error processing queued request {}: {}", request.getId(), e.getMessage());
                // Continue with next request
            }
        }
    }

    /**
     * Update the status of a request and record the change in history
     * @param requestId The ID of the request to update
     * @param newStatus The new status to set
     * @param notes Optional notes about the status change
     * @return The updated request
     * @throws RequestNotFoundException if the request is not found
     */
    @Transactional
    public Request updateRequestStatus(Long requestId, RequestStatus newStatus, String notes) 
            throws RequestNotFoundException {
        
        // Get the current username from security context
        String changedBy = getCurrentUsername();
        
        return requestRepository.findById(requestId).map(request -> {
            // Save the old status for history
            RequestStatus oldStatus = request.getStatus();
            
            // Update the status and record history
            request.updateStatus(newStatus, changedBy, notes);
            
            // If completed, update ambulance status back to available
            if (newStatus == RequestStatus.COMPLETED && request.getAmbulance() != null) {
                ambulanceService.updateAmbulanceStatus(
                        request.getAmbulance().getId(),
                        AvailabilityStatus.AVAILABLE
                );
            }
            
            return requestRepository.save(request);
            
        }).orElseThrow(() -> new RequestNotFoundException("Request not found with id: " + requestId));
    }
    
    /**
     * Get the status history for a specific request
     * @param requestId The ID of the request
     * @return List of status history entries, most recent first
     * @throws RequestNotFoundException if the request is not found
     */
    @Transactional(readOnly = true)
    public List<RequestStatusHistory> getRequestStatusHistory(Long requestId) 
            throws RequestNotFoundException {
                
        if (!requestRepository.existsById(requestId)) {
            throw new RequestNotFoundException("Request not found with id: " + requestId);
        }
        
        return statusHistoryRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }
    
    /**
     * Helper method to get the current authenticated username
     * @return The username or 'system' if not available
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
    
    // Keep the old method for backward compatibility
    public Request updateRequestStatus(Long requestId, RequestStatus status) 
            throws RequestNotFoundException {
        return updateRequestStatus(requestId, status, null);
    }

    public List<Request> getRequestsByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    public List<Request> getPendingRequests() {
        return requestRepository.findByStatus(RequestStatus.PENDING);
    }

    // Helper method for backward compatibility
    public Request createRequest(Request request) {
        return requestRepository.save(request);
    }

    public Request updateRequest(Request request) {
        return requestRepository.save(request);
    }

    /**
     * Get all requests for a specific user
     * @param user The user to get requests for
     * @return List of user's requests, ordered by request time (newest first)
     */
    @Transactional(readOnly = true)
    public List<Request> getRequestsByUser(User user) {
        return requestRepository.findByUserOrderByRequestTimeDesc(user);
    }
    
    /**
     * Get active requests for a specific user
     * Active requests are those that are not completed or cancelled
     * @param user The user to get active requests for
     * @return List of user's active requests, ordered by request time (newest first)
     */
    @Transactional(readOnly = true)
    public List<Request> getActiveRequestsByUser(User user) {
        List<RequestStatus> activeStatuses = Arrays.asList(
            RequestStatus.PENDING,
            RequestStatus.DISPATCHED,
            RequestStatus.IN_PROGRESS,
            RequestStatus.ARRIVED
        );
        return requestRepository.findByUserAndStatusInOrderByRequestTimeDesc(user, activeStatuses);
    }

    /**
     * Get all requests for a specific user with pagination
     * @param user The user to get requests for
     * @param pageable Pagination information
     * @return Page of user's requests
     */
    @Transactional(readOnly = true)
    public Page<Request> getUserRequests(User user, Pageable pageable) {
        return requestRepository.findByUser(user, pageable);
    }
    
    /**
     * Get active requests for a specific user with pagination
     * @param user The user to get active requests for
     * @param pageable Pagination information
     * @return Page of user's active requests
     */
    @Transactional(readOnly = true)
    public Page<Request> getActiveUserRequests(User user, Pageable pageable) {
        List<RequestStatus> activeStatuses = Arrays.asList(
            RequestStatus.PENDING,
            RequestStatus.DISPATCHED,
            RequestStatus.IN_PROGRESS,
            RequestStatus.ARRIVED
        );
        return requestRepository.findByUserAndStatusIn(user, activeStatuses, pageable);
    }
}