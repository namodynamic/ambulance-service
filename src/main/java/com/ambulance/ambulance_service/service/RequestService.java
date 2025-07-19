package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private AmbulanceService ambulanceService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ServiceHistoryService serviceHistoryService;

    public List<Request> getAllRequests() {
        return requestRepository.findAllByOrderByRequestTimeDesc();
    }

    public Optional<Request> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    @Transactional
    public Request createRequest(AmbulanceRequestDto ambulanceRequestDto) throws NoAvailableAmbulanceException {
        // Create new request
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

            // Update ambulance status
            ambulanceService.updateAmbulanceStatus(ambulance.getId(), AvailabilityStatus.DISPATCHED);

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

    public Request updateRequestStatus(Long requestId, RequestStatus status) throws RequestNotFoundException {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();
            request.setStatus(status);

            // If completed, update ambulance status back to available
            if (status == RequestStatus.COMPLETED && request.getAmbulance() != null) {
                ambulanceService.updateAmbulanceStatus(
                        request.getAmbulance().getId(),
                        AvailabilityStatus.AVAILABLE
                );
            }

            return requestRepository.save(request);
        } else {
            throw new RequestNotFoundException("Request not found with id: " + requestId);
        }
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