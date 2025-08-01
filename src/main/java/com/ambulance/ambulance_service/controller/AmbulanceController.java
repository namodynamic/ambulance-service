package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.service.AmbulanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ambulances")
@CrossOrigin(origins = "*")
public class AmbulanceController {
    private static final Logger logger = LoggerFactory.getLogger(AmbulanceController.class);

    private final AmbulanceService ambulanceService;

    @Autowired
    public AmbulanceController(AmbulanceService ambulanceService) {
        this.ambulanceService = ambulanceService;
    }

    @GetMapping
    public ResponseEntity<?> getAllAmbulances() {
        try {
            logger.info("Fetching all ambulances");
            List<Ambulance> ambulances = ambulanceService.getAllAmbulances();
            if (ambulances.isEmpty()) {
                logger.warn("No ambulances found in the database");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "No ambulances found"));
            }
            return ResponseEntity.ok(ambulances);
        } catch (DataAccessException e) {
            logger.error("Database error while fetching ambulances: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error accessing the database"));
        } catch (Exception e) {
            logger.error("Unexpected error while fetching ambulances: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "An unexpected error occurred"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAmbulanceById(@PathVariable Long id) {
        try {
            logger.info("Fetching ambulance with id: {}", id);
            Optional<Ambulance> ambulance = ambulanceService.getAmbulanceById(id);
            return ambulance.map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        logger.warn("Ambulance with id {} not found", id);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body((Ambulance) Collections.singletonMap("message", "Ambulance not found with id: " + id));
                    });
        } catch (Exception e) {
            logger.error("Error fetching ambulance with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error retrieving ambulance"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createAmbulance(@RequestBody Ambulance ambulance) {
        try {
            logger.info("Creating new ambulance: {}", ambulance);
            Ambulance createdAmbulance = ambulanceService.saveAmbulance(ambulance);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAmbulance);
        } catch (Exception e) {
            logger.error("Error creating ambulance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error creating ambulance: " + e.getMessage()));
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableAmbulances() {
        try {
            logger.info("Fetching available ambulances");
            List<Ambulance> availableAmbulances = ambulanceService.getAvailableAmbulances();
            if (availableAmbulances.isEmpty()) {
                logger.warn("No available ambulances found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "No available ambulances found"));
            }
            return ResponseEntity.ok(availableAmbulances);
        } catch (Exception e) {
            logger.error("Error fetching available ambulances: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error retrieving available ambulances"));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAmbulanceStatus(
            @PathVariable Long id,
            @RequestParam AvailabilityStatus status) {
        try {
            logger.info("Updating status for ambulance id: {} to status: {}", id, status);
            ambulanceService.updateAmbulanceStatus(id, status);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status update request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating ambulance status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error updating ambulance status"));
        }
    }
}