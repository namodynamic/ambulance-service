package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import com.ambulance.ambulance_service.repository.RequestStatusHistoryRepository;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RequestService implements RequestServiceInterface {
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
    @Lazy
    private ServiceHistoryRepository serviceHistoryRepository;

    @Autowired
    private RequestStatusHistoryRepository statusHistoryRepository;

    @Override
    public Page<Request> getAllRequests(Pageable pageable) {
        return requestRepository.findByDeletedFalse(pageable);
    }

    @Override
    public Optional<Request> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    @Override
    @Transactional
    public Request createRequest(AmbulanceRequestDto requestDto, com.ambulance.ambulance_service.entity.User user)
            throws NoAvailableAmbulanceException {
        logger.debug("Creating new ambulance request");
        
        // Validate request
        if (requestDto == null) {
            throw new IllegalArgumentException("Request data cannot be null");
        }
        
        // Create or find patient
        String patientName = (requestDto.getPatientName() != null && !requestDto.getPatientName().trim().isEmpty()) 
            ? requestDto.getPatientName().trim() 
            : "Unknown";
        
        logger.debug("Finding/creating patient: {}", patientName);
        // Create or find patient with initial medical notes if provided
        Patient patient;
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String initialNotes = "[" + timestamp + "] " + requestDto.getMedicalNotes().trim();
            patient = patientService.findOrCreatePatient(patientName, requestDto.getUserContact(), initialNotes);
        } else {
            patient = patientService.findOrCreatePatient(patientName, requestDto.getUserContact());
        }
        
        // Create the request with correct field names
        Request request = new Request();
        request.setUserName(patientName);
        request.setUserContact(requestDto.getUserContact());
        request.setLocation(requestDto.getLocation());
        request.setEmergencyDescription(requestDto.getEmergencyDescription());
        request.setRequestTime(LocalDateTime.now());
        
        // Set medical notes on the request if provided
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            request.setMedicalNotes(requestDto.getMedicalNotes().trim());
        }
        
        // Set the user if provided (for authenticated users)
        if (user != null) {
            request.setUser(user);
        }

        // Save the request first to ensure we have an ID
        request = requestRepository.save(request);
        
        // Save the initial status to history
        saveStatusHistory(request, null, request.getStatus(), "Request created");
        
        // Try to assign an ambulance
        logger.debug("Attempting to assign ambulance");
        Optional<Ambulance> availableAmbulance = ambulanceService.getNextAvailableAmbulance();
        
        if (availableAmbulance.isPresent()) {
            // Ambulance is available, assign it
            Ambulance ambulance = availableAmbulance.get();
            logger.debug("Found available ambulance: {}", ambulance.getId());
            
            try {
                // Update ambulance status
                ambulanceService.updateAmbulanceStatus(ambulance.getId(), AvailabilityStatus.DISPATCHED);
                
                // Update request status and assign ambulance
                request.setAmbulance(ambulance);
                request.setStatus(RequestStatus.DISPATCHED);
                request.setDispatchTime(LocalDateTime.now());
                
                // Save the updated request
                request = requestRepository.save(request);
                
                // Save status change to history
                saveStatusHistory(request, RequestStatus.PENDING, RequestStatus.DISPATCHED, 
                    "Ambulance " + ambulance.getLicensePlate() + " dispatched");
                
                // Create service history
                ServiceHistory serviceHistory = serviceHistoryService.createServiceHistory(request, patient, ambulance);
                serviceHistory.setStatus(ServiceStatus.IN_PROGRESS);
                serviceHistory.setNotes("Ambulance " + ambulance.getLicensePlate() + " dispatched to location");
                serviceHistoryRepository.save(serviceHistory);
                
                logger.info("Successfully created and dispatched request {} with ambulance {}", 
                    request.getId(), ambulance.getId());
                
                return request;
                
            } catch (Exception e) {
                logger.error("Error assigning ambulance to request: {}", e.getMessage(), e);
                // If we can't assign the ambulance, update status to PENDING
                RequestStatus oldStatus = request.getStatus();
                request.setStatus(RequestStatus.PENDING);
                request = requestRepository.save(request);
                saveStatusHistory(request, oldStatus, RequestStatus.PENDING, 
                    "Failed to assign ambulance: " + e.getMessage());
                return request;
            }
        } else {
            // No ambulances available, set status to PENDING
            logger.debug("No ambulances available, setting request to PENDING");
            RequestStatus oldStatus = request.getStatus();
            request.setStatus(RequestStatus.PENDING);
            request = requestRepository.save(request);
            saveStatusHistory(request, oldStatus, RequestStatus.PENDING, 
                "No ambulances available, request queued");
            
            // Create service history with PENDING status even when no ambulance is available
            ServiceHistory serviceHistory = serviceHistoryService.createServiceHistory(request, patient, null);
            serviceHistory.setStatus(ServiceStatus.PENDING);
            serviceHistory.setNotes("Request queued - waiting for ambulance availability");
            serviceHistoryRepository.save(serviceHistory);
            
            return request;
        }
    }

    private Request queueRequest(AmbulanceRequestDto requestDto, com.ambulance.ambulance_service.entity.User user) {
        logger.info("No ambulances available - adding request to queue");

        // Create or find patient - 'Unknown' as default name if not provided
        String patientName = (requestDto.getPatientName() != null && !requestDto.getPatientName().trim().isEmpty()) 
            ? requestDto.getPatientName().trim() 
            : "Unknown";
        
        // Create or find patient with initial medical notes if provided
        Patient patient;
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String initialNotes = "[" + timestamp + "] " + requestDto.getMedicalNotes().trim();
            patient = patientService.findOrCreatePatient(patientName, requestDto.getUserContact(), initialNotes);
        } else {
            patient = patientService.findOrCreatePatient(patientName, requestDto.getUserContact());
        }
        
        // Create the request
        Request request = new Request();
        request.setUserName(patientName);
        request.setUserContact(requestDto.getUserContact());
        request.setLocation(requestDto.getLocation());
        request.setEmergencyDescription(requestDto.getEmergencyDescription());
        request.setRequestTime(LocalDateTime.now());
        request.setStatus(RequestStatus.PENDING);
        
        // Set medical notes on the request if provided
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String initialNotes = "[" + timestamp + "] " + requestDto.getMedicalNotes().trim();
            request.setMedicalNotes(initialNotes);
        }
        
        // Set the user if provided (for authenticated users)
        if (user != null) {
            request.setUser(user);
        }

        // Save the request (without assigning an ambulance)
        request = requestRepository.save(request);

        // Save the initial status to history
        saveStatusHistory(request, null, request.getStatus(), "Request created");
        
        // Create service history with PENDING status
        ServiceHistory serviceHistory = serviceHistoryService.createServiceHistory(request, patient, null);
        serviceHistory.setStatus(ServiceStatus.PENDING);
        serviceHistoryService.updateServiceHistory(
            serviceHistory.getId(), 
            null, 
            null, 
            ServiceStatus.PENDING, 
            "Request queued - waiting for ambulance availability"
        );

        // Log the queued request
        logger.info("Request {} added to queue with service history ID: {}", request.getId(), serviceHistory.getId());
        
        return request;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processQueuedRequests() {
        List<Request> queuedRequests = requestRepository.findByStatusOrderByRequestTimeAsc(RequestStatus.PENDING);
        
        if (queuedRequests.isEmpty()) {
            return;
        }
        
        logger.info("Processing {} queued requests", queuedRequests.size());
        
        for (Request request : queuedRequests) {
            try {
                logger.debug("Processing queued request ID: {}", request.getId());
                
                // Find or create patient
                Patient patient = patientService.findOrCreatePatient(
                    request.getUserName(),
                    request.getUserContact(),
                    request.getMedicalNotes() != null ? request.getMedicalNotes() : ""
                );
                
                // Try to get an available ambulance with retry logic
                Optional<Ambulance> availableAmbulance = Optional.empty();
                for (int i = 0; i < MAX_RETRIES; i++) {
                    availableAmbulance = ambulanceService.getNextAvailableAmbulance();
                    if (availableAmbulance.isPresent()) {
                        break;
                    }
                    Thread.sleep(1000); // Wait 1 second between retries
                }
                
                if (availableAmbulance.isPresent()) {
                    Ambulance ambulance = availableAmbulance.get();
                    logger.debug("Found available ambulance ID: {} for request ID: {}", ambulance.getId(), request.getId());
                    
                    // Update ambulance status to DISPATCHED
                    try {
                        ambulanceService.updateAmbulanceStatus(ambulance.getId(), AvailabilityStatus.DISPATCHED);
                        
                        // Update request with ambulance and status
                        request.setAmbulance(ambulance);
                        request.setStatus(RequestStatus.DISPATCHED);
                        request.setDispatchTime(LocalDateTime.now());
                        
                        // Save the updated request
                        request = requestRepository.save(request);
                        
                        // Save status change to history
                        saveStatusHistory(request, RequestStatus.PENDING, RequestStatus.DISPATCHED, 
                            "Ambulance " + ambulance.getLicensePlate() + " dispatched");
                        
                        // Create or update service history
                        final Request finalRequest = request;
                        final Patient finalPatient = patient;
                        final Ambulance finalAmbulance = ambulance;
                        ServiceHistory serviceHistory = serviceHistoryRepository.findByRequestId(request.getId())
                                .stream()
                                .findFirst()
                                .orElseGet(() -> serviceHistoryService.createServiceHistory(finalRequest, finalPatient, finalAmbulance));
                        serviceHistory.setAmbulance(finalAmbulance);
                        serviceHistory.setStatus(ServiceStatus.IN_PROGRESS);
                        serviceHistory.setNotes("Ambulance " + finalAmbulance.getLicensePlate() + " dispatched to location");
                        serviceHistoryRepository.save(serviceHistory);
                        
                        logger.info("Assigned ambulance ID: {} to request ID: {}", ambulance.getId(), request.getId());
                        
                    } catch (Exception e) {
                        logger.error("Error updating ambulance status for request ID: {}: {}", 
                            request.getId(), e.getMessage(), e);
                        // Continue to next request if we can't update ambulance status
                        continue;
                    }
                } else {
                    logger.debug("No ambulances available for request ID: {} after {} retries", 
                        request.getId(), MAX_RETRIES);
                    break; // No more ambulances available, try again later
                }
                
            } catch (Exception e) {
                logger.error("Error processing queued request ID: {}: {}", 
                    request.getId(), e.getMessage(), e);
            }
        }
    }

    private void updateServiceHistoryStatus(Request request, ServiceStatus status, String notes) {
        try {
            serviceHistoryService.updateServiceStatus(
                    request.getId(),
                    status,
                    String.format("[%s] %s", LocalDateTime.now(), notes)
            );
        } catch (Exception e) {
            logger.error("Failed to update service history for request {}: {}",
                    request.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Request updateRequestStatus(Long requestId, RequestStatus status, String notes)
            throws RequestNotFoundException {
        
        String changedBy = getCurrentUsername();
        
        return requestRepository.findById(requestId).map(request -> {
            RequestStatus oldStatus = request.getStatus();
            
            // Update the status and record history
            request.setStatus(status);
            saveStatusHistory(request, oldStatus, status, notes);
            
            // Handle status-specific logic
            switch (status) {
                case DISPATCHED:
                    // If we're dispatching, assign an ambulance if not already assigned
                    if (request.getAmbulance() == null) {
                        Optional<Ambulance> ambulance = ambulanceService.getNextAvailableAmbulance();
                        if (ambulance.isPresent()) {
                            request.setAmbulance(ambulance.get());
                            request.setDispatchTime(LocalDateTime.now());
                            ambulanceService.updateAmbulanceStatus(
                                ambulance.get().getId(), 
                                AvailabilityStatus.DISPATCHED
                            );
                        }
                    }
                    updateServiceHistoryStatus(
                        request,
                        ServiceStatus.IN_PROGRESS,
                        "Ambulance dispatched: " + (notes != null ? notes : "")
                    );
                    break;
                    
                case IN_PROGRESS:
                    updateServiceHistoryStatus(
                        request,
                        ServiceStatus.IN_PROGRESS,
                        "Request in progress: " + (notes != null ? notes : "")
                    );
                    break;
                    
                case ARRIVED:
                    updateServiceHistoryStatus(
                        request,
                        ServiceStatus.ARRIVED,
                        "Ambulance arrived at location: " + (notes != null ? notes : "")
                    );
                    break;
                    
                case COMPLETED:
                    // Update ambulance status back to available
                    if (request.getAmbulance() != null) {
                        ambulanceService.updateAmbulanceStatus(
                            request.getAmbulance().getId(),
                            AvailabilityStatus.AVAILABLE
                        );
                    }
                    updateServiceHistoryStatus(
                        request,
                        ServiceStatus.COMPLETED,
                        "Request completed: " + (notes != null ? notes : "")
                    );
                    break;
                    
                case CANCELLED:
                    // If ambulance was assigned, make it available
                    if (request.getAmbulance() != null) {
                        ambulanceService.updateAmbulanceStatus(
                            request.getAmbulance().getId(),
                            AvailabilityStatus.AVAILABLE
                        );
                    }
                    updateServiceHistoryStatus(
                        request,
                        ServiceStatus.CANCELLED,
                        "Request cancelled: " + (notes != null ? notes : "")
                    );
                    break;
            }
            
            // Save the updated request
            return requestRepository.save(request);
            
        }).orElseThrow(() -> new RequestNotFoundException("Request not found with id: " + requestId));
    }

    @Override
    public boolean permanentlyDeleteRequest(Long id) {
        return requestRepository.findById(id)
                .map(request -> {
                    // First delete the status history
                    deleteStatusHistoryForRequest(id);
                    // Then delete the request
                    requestRepository.deleteById(id);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Deletes all status history entries for a request
     * @param requestId The ID of the request
     */
    private void deleteStatusHistoryForRequest(Long requestId) {
        List<RequestStatusHistory> history = statusHistoryRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
        statusHistoryRepository.deleteAll(history);
    }

    /**
     * Saves the status history for a request
     * @param request The request to save history for
     * @param oldStatus The previous status
     * @param newStatus The new status
     * @param notes Additional notes about the status change
     */
    public void saveStatusHistory(Request request, RequestStatus oldStatus, RequestStatus newStatus, String notes) {
        RequestStatusHistory history = new RequestStatusHistory();
        history.setRequest(request);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setNotes(notes);
        history.setChangedBy(getCurrentUsername());
        statusHistoryRepository.save(history);
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

    @Override
    public List<RequestStatusHistory> getRequestStatusHistory(Long requestId) 
            throws RequestNotFoundException {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found with id: " + requestId));
        return statusHistoryRepository.findByRequestOrderByCreatedAtDesc(request);
    }

    @Override
    public List<Request> getRequestsByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    @Override
    public List<Request> getPendingRequests() {
        return requestRepository.findByStatus(RequestStatus.PENDING);
    }

    @Override
    public List<Request> getRequestsByUser(User user) {
        return requestRepository.findByUserOrderByRequestTimeDesc(user);
    }

    @Override
    public List<Request> getActiveRequestsByUser(User user) {
        List<RequestStatus> activeStatuses = Arrays.asList(
            RequestStatus.PENDING,
            RequestStatus.DISPATCHED,
            RequestStatus.IN_PROGRESS,
            RequestStatus.ARRIVED
        );
        return requestRepository.findByUserAndStatusInOrderByRequestTimeDesc(user, activeStatuses);
    }

    @Override
    public Page<Request> getActiveUserRequests(User user, Pageable pageable) {
        List<RequestStatus> activeStatuses = Arrays.asList(
            RequestStatus.PENDING,
            RequestStatus.DISPATCHED,
            RequestStatus.IN_PROGRESS,
            RequestStatus.ARRIVED
        );
        return requestRepository.findByUserAndStatusIn(user, activeStatuses, pageable);
    }

    @Override
    public long countAllRequests() {
        return requestRepository.count();
    }

    @Override
    public long countRequestsByStatus(String status) {
        try {
            RequestStatus requestStatus = RequestStatus.valueOf(status.toUpperCase());
            return requestRepository.countByStatus(requestStatus);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value: {}", status);
            return 0;
        }
    }

    @Override
    public Request createRequest(Request request) {
        // Set default values if not provided
        if (request.getStatus() == null) {
            request.setStatus(RequestStatus.PENDING);
        }
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(LocalDateTime.now());
        }
        
        // Save the request
        Request savedRequest = requestRepository.save(request);
        
        // Save the initial status to history
        saveStatusHistory(savedRequest, null, savedRequest.getStatus(), "Request created");
        
        return savedRequest;
    }

    @Override
    public Request updateRequest(Request request) {
        return requestRepository.findById(request.getId())
                .map(existingRequest -> {
                    // Preserve the original creation timestamp
                    LocalDateTime originalCreatedAt = existingRequest.getCreatedAt();
                    
                    // Update all fields from the provided request
                    if (request.getStatus() != null) {
                        existingRequest.setStatus(request.getStatus());
                        // Record status change in history
                        saveStatusHistory(existingRequest, existingRequest.getStatus(), request.getStatus(), "Request updated");
                    }
                    
                    // Update basic fields
                    if (request.getUserName() != null) {
                        existingRequest.setUserName(request.getUserName());
                    }
                    
                    if (request.getUserContact() != null) {
                        existingRequest.setUserContact(request.getUserContact());
                    }
                    
                    if (request.getLocation() != null) {
                        existingRequest.setLocation(request.getLocation());
                    }
                    
                    if (request.getEmergencyDescription() != null) {
                        existingRequest.setEmergencyDescription(request.getEmergencyDescription());
                    }
                    
                    if (request.getMedicalNotes() != null) {
                        existingRequest.setMedicalNotes(request.getMedicalNotes());
                    }
                    
                    if (request.getAmbulance() != null) {
                        existingRequest.setAmbulance(request.getAmbulance());
                    }
                    
                    if (request.getRequestTime() != null) {
                        existingRequest.setRequestTime(request.getRequestTime());
                    }
                    
                    if (request.getDispatchTime() != null) {
                        existingRequest.setDispatchTime(request.getDispatchTime());
                    }
                    
                    // Restore the original creation timestamp
                    existingRequest.setCreatedAt(originalCreatedAt);
                    
                    return requestRepository.save(existingRequest);
                })
                .orElseThrow(() -> new RequestNotFoundException("Request not found with id: " + request.getId()));
    }
    
    @Override
    public boolean deleteRequest(Long id) {
        return requestRepository.findById(id)
                .map(request -> {
                    if (request.isDeleted()) {
                        return false; // Already deleted
                    }
                    request.setDeleted(true);
                    requestRepository.save(request);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public Optional<Request> findByIdIncludingDeleted(Long id) {
        return requestRepository.findById(id);
    }

    @Override
    public List<Request> findAllIncludingDeleted() {
        return requestRepository.findAll();
    }

    @Override
    public Page<Request> findAllIncludingDeleted(Pageable pageable) {
        return requestRepository.findAll(pageable);
    }

    @Override
    public boolean restoreRequest(Long id) {
        return requestRepository.findById(id)
                .map(request -> {
                    if (!request.isDeleted()) {
                        return false;
                    }
                    request.setDeleted(false);
                    request.setDeletedAt(null);
                    requestRepository.save(request);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Marks an ambulance as arrived at the location and updates the request status if needed.
     * This is a specialized method for handling ambulance arrival events.
     *
     * @param requestId The ID of the request to update
     * @param notes Additional notes about the arrival
     * @throws RequestNotFoundException if the request is not found
     */
    @Transactional
    public void markAmbulanceArrived(Long requestId, String notes) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found with id: " + requestId));

        // Update service history with arrival information
        updateServiceHistoryStatus(
                request,
                ServiceStatus.ARRIVED,
                "Ambulance arrived at location: " + (notes != null ? notes : "")
        );

        // If request is not already in progress, update its status
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            updateRequestStatus(
                    requestId,
                    RequestStatus.IN_PROGRESS,
                    "Ambulance arrived at location: " + (notes != null ? notes : "")
            );
        } else {
            // save the request to ensure service history is persisted
            requestRepository.save(request);
        }
    }
}