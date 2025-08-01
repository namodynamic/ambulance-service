package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.dto.ServiceHistoryDTO;
import com.ambulance.ambulance_service.entity.ServiceHistory;
import com.ambulance.ambulance_service.exception.EntityNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServiceHistoryService {


    private final RequestService requestService;
    private final ServiceHistoryRepository serviceHistoryRepository;
    private final RequestRepository requestRepository;
    private final AmbulanceService ambulanceService;

    @Autowired
    public ServiceHistoryService(
            ServiceHistoryRepository serviceHistoryRepository,
            @Lazy RequestService requestService,
            RequestRepository requestRepository,
            AmbulanceService ambulanceService
    ) {
        this.serviceHistoryRepository = serviceHistoryRepository;
        this.requestService = requestService;
        this.requestRepository = requestRepository;
        this.ambulanceService = ambulanceService;
    }


    public List<ServiceHistoryDTO> getAllServiceHistory() {
        return serviceHistoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // helper method to convert entity to DTO
    public ServiceHistoryDTO convertToDTO(ServiceHistory history) {
        if (history == null) {
            return null;
        }

        ServiceHistoryDTO dto = new ServiceHistoryDTO();
        dto.setId(history.getId());
        dto.setRequestId(history.getRequest() != null ? history.getRequest().getId() : null);
        dto.setAmbulanceId(history.getAmbulance() != null ? history.getAmbulance().getId() : null);
        dto.setPatientId(history.getPatient() != null ? history.getPatient().getId() : null);
        dto.setStatus(history.getStatus());
        dto.setNotes(history.getNotes());
        dto.setArrivalTime(history.getArrivalTime());
        dto.setCompletionTime(history.getCompletionTime());
        dto.setCreatedAt(history.getCreatedAt());
        return dto;
    }


    public Optional<ServiceHistory> getServiceHistoryById(Long id) {
        return serviceHistoryRepository.findById(id);
    }

    public ServiceHistory createServiceHistory(Request request, Patient patient, Ambulance ambulance) {
        ServiceHistory serviceHistory = new ServiceHistory(request, patient, ambulance);
        serviceHistory.setStatus(ServiceStatus.IN_PROGRESS);
        return serviceHistoryRepository.save(serviceHistory);
    }

    public ServiceHistory updateServiceHistory(Long id, LocalDateTime arrivalTime,
                                               LocalDateTime completionTime, ServiceStatus status, String notes) {
        ServiceHistory history = serviceHistoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service history not found with id: " + id));

        // Only validate if status is changing
        if (status != null && status != history.getStatus()) {
            validateStatusTransition(history.getStatus(), status);
        }

        if (arrivalTime != null) history.setArrivalTime(arrivalTime);
        if (completionTime != null) history.setCompletionTime(completionTime);
        if (status != null) history.setStatus(status);
        if (notes != null) {
            String newNotes = history.getNotes() != null
                    ? history.getNotes() + "\n" + notes
                    : notes;
            history.setNotes(newNotes);
        }

        return serviceHistoryRepository.save(history);
    }

    public List<ServiceHistory> getServiceHistoryByStatus(ServiceStatus status) {
        return serviceHistoryRepository.findByStatus(status);
    }

    public List<ServiceHistory> getServiceHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        return serviceHistoryRepository.findByCreatedAtBetween(start, end);
    }

    public void updateServiceStatus(Long requestId, ServiceStatus newStatus, String notes) {

        List<ServiceHistory> histories = serviceHistoryRepository.findByRequestId(requestId);
        ServiceHistory history;

        if (histories == null || histories.isEmpty()) {
            history = serviceHistoryRepository.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("Service history not found for request: " + requestId));
        } else {
            // Get the latest history if multiple exist
            history = histories.stream()
                    .max((h1, h2) -> h1.getCreatedAt().compareTo(h2.getCreatedAt()))
                    .orElse(histories.get(0));
        }

        validateStatusTransition(history.getStatus(), newStatus);
        updateStatusWithTimestamps(history, newStatus, notes);
        serviceHistoryRepository.save(history);
    }

    private void validateStatusTransition(ServiceStatus currentStatus, ServiceStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }


        if (currentStatus == newStatus) {
            return;
        }

        //  transitions
        switch (currentStatus) {
            case PENDING:
                if (!List.of(ServiceStatus.IN_PROGRESS, ServiceStatus.CANCELLED).contains(newStatus)) {
                    throw new IllegalStateException(
                            String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
                    );
                }
                break;

            case IN_PROGRESS:
                if (!List.of(ServiceStatus.ARRIVED, ServiceStatus.COMPLETED, ServiceStatus.CANCELLED).contains(newStatus)) {
                    throw new IllegalStateException(
                            String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
                    );
                }
                break;

            case ARRIVED:
                if (!List.of(ServiceStatus.IN_PROGRESS, ServiceStatus.COMPLETED, ServiceStatus.CANCELLED).contains(newStatus)) {
                    throw new IllegalStateException(
                            String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
                    );
                }
                break;

            case COMPLETED:
            case CANCELLED:
                throw new IllegalStateException(
                        String.format("Cannot change status from %s to %s", currentStatus, newStatus)
                );
        }
    }

    private void updateStatusWithTimestamps(ServiceHistory history, ServiceStatus newStatus, String notes) {
        LocalDateTime now = LocalDateTime.now();

        // Update status and notes
        history.setStatus(newStatus);
        history.setNotes(notes);

        // Update timestamps based on status
        switch (newStatus) {
            case IN_PROGRESS:
                break;
            case ARRIVED:
                history.setArrivalTime(now);
                break;
            case COMPLETED:
            case CANCELLED:
                if (history.getArrivalTime() == null) {
                    history.setArrivalTime(now);
                }
                history.setCompletionTime(now);
                break;
        }
    }

    public List<ServiceHistory> getServiceHistoryByRequestId(Long id) {
        return serviceHistoryRepository.findByRequestId(id);
    }

    @Transactional
    public void updateStatusAndSync(Long serviceHistoryId, String statusStr, String notes) {
        ServiceHistory history = serviceHistoryRepository.findById(serviceHistoryId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceHistory not found"));

        ServiceStatus newStatus = ServiceStatus.valueOf(statusStr);


        history.setStatus(newStatus);
        if (notes != null) history.setNotes(notes);
        serviceHistoryRepository.save(history);


        Request request = history.getRequest();
        if (request != null) {

            RequestStatus reqStatus = mapServiceStatusToRequestStatus(newStatus);
            if (reqStatus != null && reqStatus != request.getStatus()) {
                request.setStatus(reqStatus);

                requestService.saveStatusHistory(request, request.getStatus(), reqStatus, notes);
                requestRepository.save(request);
            }
        }


        if (history.getAmbulance() != null) {
            AvailabilityStatus ambStatus = mapServiceStatusToAmbulanceAvailability(newStatus);
            if (ambStatus != null) {
                ambulanceService.updateAmbulanceStatus(history.getAmbulance().getId(), ambStatus);
            }
        }
    }

    // Helper mapping methods for status sync
    private RequestStatus mapServiceStatusToRequestStatus(ServiceStatus status) {
        if (status == null) return null;
        return switch (status) {
            case PENDING -> RequestStatus.PENDING;
            case IN_PROGRESS -> RequestStatus.IN_PROGRESS;
            case ARRIVED -> RequestStatus.ARRIVED;
            case COMPLETED -> RequestStatus.COMPLETED;
            case CANCELLED -> RequestStatus.CANCELLED;
            default -> null;
        };
    }
    private AvailabilityStatus mapServiceStatusToAmbulanceAvailability(ServiceStatus status) {
        if (status == null) return null;
        return switch (status) {
            case PENDING, COMPLETED, CANCELLED -> AvailabilityStatus.AVAILABLE;
            case IN_PROGRESS, ARRIVED -> AvailabilityStatus.DISPATCHED;
            default -> null;
        };
    }
}