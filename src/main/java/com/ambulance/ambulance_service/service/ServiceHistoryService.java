package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ServiceHistoryService {

    @Autowired
    private ServiceHistoryRepository serviceHistoryRepository;

    public List<ServiceHistory> getAllServiceHistory() {
        return serviceHistoryRepository.findAll();
    }

    public Optional<ServiceHistory> getServiceHistoryById(Long id) {
        return serviceHistoryRepository.findById(id);
    }

    public ServiceHistory createServiceHistory(Request request, Patient patient, Ambulance ambulance) {
        ServiceHistory serviceHistory = new ServiceHistory(request, patient, ambulance);
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