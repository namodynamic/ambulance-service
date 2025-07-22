package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import com.ambulance.ambulance_service.repository.RequestStatusHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RequestService {
    private static final int MAX_RETRIES = 3;

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
    public Request createRequest(AmbulanceRequestDto ambulanceRequestDto)
            throws NoAvailableAmbulanceException {

        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return createRequestTransaction(ambulanceRequestDto);
            } catch (ObjectOptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    break;
                }
                // Log and retry
                System.out.println("Retrying ambulance assignment (attempt " + attempt + ")");
            }
        }
        
        // If we get here, all retries failed - queue the request
        return queueRequest(ambulanceRequestDto);
    }

    private Request createRequestTransaction(AmbulanceRequestDto ambulanceRequestDto) throws NoAvailableAmbulanceException {
        Request request = new Request(
                ambulanceRequestDto.getUserName(),
                ambulanceRequestDto.getUserContact(),
                ambulanceRequestDto.getLocation(),
                ambulanceRequestDto.getEmergencyDescription()
        );

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
                    ambulanceRequestDto.getUserName(),
                    ambulanceRequestDto.getUserContact()
            );

            // Create service history
            serviceHistoryService.createServiceHistory(request, patient, ambulance);

            return requestRepository.save(request);
        } else {
            throw new NoAvailableAmbulanceException("No ambulance available at the moment");
        }
    }

    private Request queueRequest(AmbulanceRequestDto ambulanceRequestDto) {
        System.out.println("No ambulances available - adding request to queue");
        
        // Create and save the request with PENDING status
        Request request = new Request(
                ambulanceRequestDto.getUserName(),
                ambulanceRequestDto.getUserContact(),
                ambulanceRequestDto.getLocation(),
                ambulanceRequestDto.getEmergencyDescription()
        );
        request.setStatus(RequestStatus.PENDING);
        
        // Save the request (without assigning an ambulance)
        request = requestRepository.save(request);
        
        // Log the queued request
        System.out.println("Request " + request.getId() + " added to queue");
        
        return request;
    }

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    @Transactional
    public void processQueuedRequests() {
        List<Request> queuedRequests = requestRepository.findByStatusOrderByRequestTimeAsc(RequestStatus.PENDING);
        
        if (queuedRequests.isEmpty()) {
            return;
        }
        
        System.out.println("Processing " + queuedRequests.size() + " queued requests");
        
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
                    
                    System.out.println("Assigned ambulance " + ambulance.getId() + " to request " + request.getId());
                } else {
                    System.out.println("No ambulances available for request " + request.getId());
                    break; // No more ambulances available, try again later
                }
            } catch (Exception e) {
                System.err.println("Error processing queued request " + request.getId() + ": " + e.getMessage());
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
}