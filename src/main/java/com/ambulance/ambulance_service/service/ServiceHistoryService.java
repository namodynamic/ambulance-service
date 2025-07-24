package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.dto.ServiceHistoryDTO;
import com.ambulance.ambulance_service.entity.ServiceHistory;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServiceHistoryService {

    @Autowired
    private ServiceHistoryRepository serviceHistoryRepository;

    public List<ServiceHistoryDTO> getAllServiceHistory() {
        return serviceHistoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Add this helper method to convert entity to DTO
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
        Optional<ServiceHistory> serviceHistoryOpt = serviceHistoryRepository.findById(id);
        if (serviceHistoryOpt.isPresent()) {
            ServiceHistory serviceHistory = serviceHistoryOpt.get();
            if (arrivalTime != null) serviceHistory.setArrivalTime(arrivalTime);
            if (completionTime != null) serviceHistory.setCompletionTime(completionTime);
            if (status != null) serviceHistory.setStatus(status);
            if (notes != null) serviceHistory.setNotes(notes);

            return serviceHistoryRepository.save(serviceHistory);
        }
        return null;
    }

    public List<ServiceHistory> getServiceHistoryByStatus(ServiceStatus status) {
        return serviceHistoryRepository.findByStatus(status);
    }

    public List<ServiceHistory> getServiceHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        return serviceHistoryRepository.findByCreatedAtBetween(start, end);
    }
}