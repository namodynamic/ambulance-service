package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import com.ambulance.ambulance_service.repository.RequestStatusHistoryRepository;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
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
    private ServiceHistoryRepository serviceHistoryRepository;

    @Autowired
    private RequestStatusHistoryRepository statusHistoryRepository;

    @Override
    public List<Request> getAllRequests() {
        return requestRepository.findAllByOrderByRequestTimeDesc();
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
        Patient patient = patientService.findOrCreatePatient(patientName, requestDto.getUserContact());
        
        // Update patient's medical notes if provided
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String updatedNotes = patient.getMedicalNotes() != null 
                ? patient.getMedicalNotes() + "\n[" + timestamp + "] " + requestDto.getMedicalNotes().trim()
                : "[" + timestamp + "] " + requestDto.getMedicalNotes().trim();
            patient.setMedicalNotes(updatedNotes);
            patient = patientService.savePatient(patient);
        }
        
        // Create the request with correct field names
        Request request = new Request();
        request.setUserName(patientName);
        request.setUserContact(requestDto.getUserContact());
        request.setLocation(requestDto.getLocation());
        request.setEmergencyDescription(requestDto.getEmergencyDescription());
        request.setRequestTime(LocalDateTime.now());
        
        // Set medical notes if provided
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            request.setMedicalNotes(requestDto.getMedicalNotes().trim());
        }
        
        // Set the user if provided (for authenticated users)
        if (user != null) {
            request.setUser(user);
        }

        // Save the request first to ensure we have an ID
        request = requestRepository.save(request);
        
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
                request.setStatus(RequestStatus.PENDING);
                request = requestRepository.save(request);
                
                // Create service history with PENDING status
                ServiceHistory serviceHistory = serviceHistoryService.createServiceHistory(request, patient, null);
                serviceHistory.setStatus(ServiceStatus.PENDING);
                serviceHistory.setNotes("No ambulances available, request queued");
                serviceHistoryRepository.save(serviceHistory);
                
                return request;
            }
        } else {
            // No ambulances available, set status to PENDING
            logger.debug("No ambulances available, setting request to PENDING");
            request.setStatus(RequestStatus.PENDING);
            request = requestRepository.save(request);
            
            // Create service history with PENDING status
            ServiceHistory serviceHistory = serviceHistoryService.createServiceHistory(request, patient, null);
            serviceHistory.setStatus(ServiceStatus.PENDING);
            serviceHistory.setNotes("No ambulances available, request queued");
            serviceHistoryRepository.save(serviceHistory);
            
            return request;
        }
    }

    private Request queueRequest(AmbulanceRequestDto requestDto, com.ambulance.ambulance_service.entity.User user) {
        logger.info("No ambulances available - adding request to queue");

        // Create or find patient - use 'Unknown' as default name if not provided
        String patientName = (requestDto.getPatientName() != null && !requestDto.getPatientName().trim().isEmpty()) 
            ? requestDto.getPatientName().trim() 
            : "Unknown";
        
        // Create or find patient
        Patient patient = patientService.findOrCreatePatient(
            patientName,
            requestDto.getUserContact()
        );
        
        // Update patient's medical notes if provided
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String updatedNotes = patient.getMedicalNotes() != null 
                ? patient.getMedicalNotes() + "\n[" + timestamp + "] " + requestDto.getMedicalNotes().trim()
                : "[" + timestamp + "] " + requestDto.getMedicalNotes().trim();
            patient.setMedicalNotes(updatedNotes);
            patient = patientService.savePatient(patient);
        }
        
        // Create the request
        Request request = new Request();
        request.setUserName(patientName);
        request.setUserContact(requestDto.getUserContact());
        request.setLocation(requestDto.getLocation());
        request.setEmergencyDescription(requestDto.getEmergencyDescription());
        request.setRequestTime(LocalDateTime.now());
        request.setStatus(RequestStatus.PENDING);
        
        // Set medical notes if provided
        if (requestDto.getMedicalNotes() != null && !requestDto.getMedicalNotes().trim().isEmpty()) {
            request.setMedicalNotes(requestDto.getMedicalNotes().trim());
        }

        // Set the user if provided (for authenticated users)
        if (user != null) {
            request.setUser(user);
        }

        // Save the request (without assigning an ambulance)
        request = requestRepository.save(request);

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
                logger.debug("Processing queued request ID: {}", request.getId());
                
                // Find or create patient - use getUserName() instead of getPatientName()
                Patient patient = patientService.findOrCreatePatient(
                    request.getUserName(),
                    request.getUserContact()
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
                // Continue with next request on error
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
            // Consider whether to rethrow or handle differently based on your requirements
        }
    }

    @Override
    public Request updateRequestStatus(Long requestId, RequestStatus status, String notes)
            throws RequestNotFoundException {

        String changedBy = getCurrentUsername();

        return requestRepository.findById(requestId).map(request -> {
            RequestStatus oldStatus = request.getStatus();

            // Update the status and record history
            request.updateStatus(status, changedBy, notes);

            // Update service history based on status
            switch (status) {
                case DISPATCHED:
                    updateServiceHistoryStatus(
                            request,
                            ServiceStatus.IN_PROGRESS,
                            "Ambulance dispatched: " + notes
                    );
                    break;

                case IN_PROGRESS:
                    updateServiceHistoryStatus(
                            request,
                            ServiceStatus.IN_PROGRESS,
                            "Request in progress: " + notes
                    );
                    break;

                case ARRIVED:
                    updateServiceHistoryStatus(
                            request,
                            ServiceStatus.ARRIVED,
                            "Ambulance arrived at location: " + notes
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
                            "Request completed: " + notes
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
                            "Request cancelled: " + notes
                    );
                    break;
            }

            return requestRepository.save(request);

        }).orElseThrow(() -> new RequestNotFoundException("Request not found with id: " + requestId));
    }


    public void markAmbulanceArrived(Long requestId, String notes) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found with id: " + requestId));

        // Update service history
        updateServiceHistoryStatus(
                request,
                ServiceStatus.ARRIVED,
                "Ambulance arrived at location: " + notes
        );

        // Update request status if needed
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            request.updateStatus(
                    RequestStatus.IN_PROGRESS,
                    getCurrentUsername(),
                    "Ambulance arrived at location"
            );
            requestRepository.save(request);
        }
    }
    
    @Override
    public List<RequestStatusHistory> getRequestStatusHistory(Long requestId) 
            throws RequestNotFoundException {
                
        if (!requestRepository.existsById(requestId)) {
            throw new RequestNotFoundException("Request not found with id: " + requestId);
        }
        
        return statusHistoryRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
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
        return requestRepository.save(request);
    }

    @Override
    public Request updateRequest(Request request) {
        return requestRepository.save(request);
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
}