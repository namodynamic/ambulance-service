package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.entity.ServiceHistory;
import com.ambulance.ambulance_service.entity.ServiceStatus;
import com.ambulance.ambulance_service.service.ServiceHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/service-history")
@CrossOrigin(origins = "*")
public class ServiceHistoryController {

    @Autowired
    private ServiceHistoryService serviceHistoryService;

    @GetMapping
    public List<ServiceHistory> getAllServiceHistory() {
        return serviceHistoryService.getAllServiceHistory();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceHistory> getServiceHistoryById(@PathVariable Long id) {
        Optional<ServiceHistory> serviceHistory = serviceHistoryService.getServiceHistoryById(id);
        return serviceHistory.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceHistory> updateServiceHistory(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime arrivalTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime completionTime,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) String notes) {

        ServiceHistory updated = serviceHistoryService.updateServiceHistory(id, arrivalTime, completionTime, status, notes);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    public List<ServiceHistory> getServiceHistoryByStatus(@PathVariable ServiceStatus status) {
        return serviceHistoryService.getServiceHistoryByStatus(status);
    }

    @GetMapping("/date-range")
    public List<ServiceHistory> getServiceHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return serviceHistoryService.getServiceHistoryByDateRange(start, end);
    }
}