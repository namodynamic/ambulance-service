package com.ambulance.ambulance_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ambulance.ambulance_service.service.AmbulanceService;
import com.ambulance.ambulance_service.service.RequestService;
import com.ambulance.ambulance_service.service.PatientService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugController {

    @Autowired
    private AmbulanceService ambulanceService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private PatientService patientService;

    /**
     * Health check endpoint to verify API is working
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "API is working correctly");
        return ResponseEntity.ok(response);
    }

    /**
     * Get basic statistics for debugging
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("ambulanceCount", ambulanceService.getAllAmbulances().size());
            stats.put("availableAmbulances", ambulanceService.getAvailableAmbulances().size());
        } catch (Exception e) {
            stats.put("ambulanceError", e.getMessage());
        }

        try {
            // Use a large page size to get all requests at once for the debug endpoint
            Pageable pageable = PageRequest.of(0, 1000);
            stats.put("requestCount", requestService.getAllRequests(pageable).getTotalElements());
            stats.put("pendingRequests", requestService.getPendingRequests().size());
        } catch (Exception e) {
            stats.put("requestError", e.getMessage());
        }

        try {
            stats.put("patientCount", patientService.getAllPatients().size());
        } catch (Exception e) {
            stats.put("patientError", e.getMessage());
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * Test endpoint that doesn't require authentication
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Test endpoint working");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
}