package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.dto.AdminDashboardStats;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.EntityNotFoundException;
import com.ambulance.ambulance_service.repository.PatientRepository;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
import com.ambulance.ambulance_service.repository.UserRepository;
import com.ambulance.ambulance_service.service.AmbulanceService;
import com.ambulance.ambulance_service.service.PatientService;
import com.ambulance.ambulance_service.service.RequestService;
import com.ambulance.ambulance_service.service.ServiceHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.ambulance.ambulance_service.dto.RequestDTO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RequestService requestService;
    private final AmbulanceService ambulanceService;
    private final PatientService patientService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ServiceHistoryRepository serviceHistoryRepository;
    private final ServiceHistoryService serviceHistoryService;

    @Autowired
    public AdminController(RequestService requestService,
                          AmbulanceService ambulanceService,
                           UserRepository userRepository,
                           PatientRepository patientRepository,
                           ServiceHistoryRepository serviceHistoryRepository,
                           ServiceHistoryService serviceHistoryService,
                          PatientService patientService) {
        this.requestService = requestService;
        this.ambulanceService = ambulanceService;
        this.patientService = patientService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.serviceHistoryRepository = serviceHistoryRepository;
        this.serviceHistoryService = serviceHistoryService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStats> getDashboardStats() {
        AdminDashboardStats stats = new AdminDashboardStats();

        // Get request statistics
        stats.setTotalRequests(requestService.countAllRequests());
        stats.setPendingRequests(requestService.countRequestsByStatus("PENDING"));
        stats.setCompletedRequests(requestService.countRequestsByStatus("COMPLETED"));
        stats.setInProgressRequests(requestService.countRequestsByStatus("IN_PROGRESS"));

        // Get ambulance statistics
        stats.setTotalAmbulances(ambulanceService.countAllAmbulances());
        stats.setAvailableAmbulances(ambulanceService.countAmbulancesByStatus(AvailabilityStatus.AVAILABLE));
        stats.setDispatchedAmbulances(ambulanceService.countAmbulancesByStatus(AvailabilityStatus.DISPATCHED));

        // Get patient statistics
        stats.setTotalPatients(patientService.countAllPatients());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getAllRequests(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean includeDeleted) {
        
        // If pagination parameters are provided, return paginated results
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            if (Boolean.TRUE.equals(includeDeleted)) {
                return ResponseEntity.ok(requestService.findAllIncludingDeleted(pageable));
            }
            return ResponseEntity.ok(requestService.getAllRequests(pageable));
        }
        
        // If no pagination parameters, use a large page size to get all results
        Pageable pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());
        if (Boolean.TRUE.equals(includeDeleted)) {
            return ResponseEntity.ok(requestService.findAllIncludingDeleted(pageable).getContent());
        }
        return ResponseEntity.ok(requestService.getAllRequests(pageable).getContent());
    }

    @PostMapping("/requests")
    public ResponseEntity<Request> createRequest(@RequestBody Request request) {
        try {
            Request createdRequest = requestService.createRequest(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<Request> getRequestById(@PathVariable Long id) {
        return requestService.getRequestById(id)
                .map(ResponseEntity::ok)
                .or(() -> requestService.findByIdIncludingDeleted(id)
                        .map(ResponseEntity::ok))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/requests/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id,
                                           @RequestBody Request requestDetails) {
        // Ensures the ID in the path matches the ID in the request body if provided
        if (requestDetails.getId() != null && !requestDetails.getId().equals(id)) {
            return ResponseEntity.badRequest().body("ID in path does not match ID in request body");
        }

        // Set the ID from path if not provided in the request body
        if (requestDetails.getId() == null) {
            requestDetails.setId(id);
        }

        try {
            Request updatedRequest = requestService.updateRequest(requestDetails);
            return ResponseEntity.ok(updatedRequest);
        } catch (RequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        boolean deleted = requestService.deleteRequest(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/requests/{id}/restore")
    public ResponseEntity<Void> restoreRequest(@PathVariable Long id) {
        boolean restored = requestService.restoreRequest(id);
        if (restored) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/requests/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteRequest(@PathVariable Long id) {
        boolean deleted = requestService.permanentlyDeleteRequest(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<?> getRequestsByUserId(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<Request> requests = requestService.getRequestsByUser(userOpt.get());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/ambulances")
    public ResponseEntity<List<Ambulance>> getAllAmbulances() {
        return ResponseEntity.ok(ambulanceService.getAllAmbulances());
    }

    @PostMapping("/ambulances")
    public ResponseEntity<Ambulance> createAmbulance(@RequestBody Ambulance ambulance) {
        try {
            // Check if license plate already exists (including deleted ones)
            Optional<Ambulance> existingAmbulance = ambulanceService.findByLicensePlateIncludingDeleted(ambulance.getLicensePlate());
            if (existingAmbulance.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            Ambulance createdAmbulance = ambulanceService.createAmbulance(ambulance);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAmbulance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ambulances/{id}")
    public ResponseEntity<Ambulance> getAmbulanceById(@PathVariable Long id) {
        return ambulanceService.getAmbulanceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/ambulances/{id}")
    public ResponseEntity<?> updateAmbulance(@PathVariable Long id, @RequestBody Ambulance ambulanceDetails) {
        // Ensures the ID in the path matches the ID in the request body if provided
        if (ambulanceDetails.getId() != null && !ambulanceDetails.getId().equals(id)) {
            return ResponseEntity.badRequest().body("ID in path does not match ID in request body");
        }

        // Check if license plate is being changed to one that already exists
        if (ambulanceDetails.getLicensePlate() != null) {
            Optional<Ambulance> existingWithSamePlate = ambulanceService.findByLicensePlateIncludingDeleted(ambulanceDetails.getLicensePlate());
            if (existingWithSamePlate.isPresent() && !existingWithSamePlate.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("An ambulance with this license plate already exists");
            }
        }

        return ambulanceService.updateAmbulance(id, ambulanceDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/ambulances/{id}")
    public ResponseEntity<Void> deleteAmbulance(@PathVariable Long id) {
        boolean deleted = ambulanceService.deleteAmbulance(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/patients")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @DeleteMapping("/patients/{id}/soft-delete")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        boolean deleted = patientService.deletePatient(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/patients/{id}")
    public ResponseEntity<?> permanentlyDeletePatient(@PathVariable Long id) {
        try {
            boolean deleted = patientService.permanentlyDeletePatient(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/patients/{patientId}/requests")
    public ResponseEntity<?> getRequestsByPatientId(@PathVariable Long patientId) {
        List<ServiceHistory> histories = serviceHistoryRepository.findByPatientId(patientId);
        List<RequestDTO> requests = histories.stream()
                .map(ServiceHistory::getRequest)
                .map(RequestDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/patients/{id}")
    public ResponseEntity<?> updatePatient(@PathVariable Long id, @RequestBody Patient patientDetails) {
        // Ensures the ID in the path matches the ID in the request body if provided
        if (patientDetails.getId() != null && !patientDetails.getId().equals(id)) {
            return ResponseEntity.badRequest().body("ID in path does not match ID in request body");
        }

        return patientService.updatePatient(id, patientDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/service-history/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminUpdateServiceHistoryStatus(
            @PathVariable Long id,
            @RequestBody ServiceHistoryController.ServiceHistoryStatusUpdateRequest request) {
        try {
            serviceHistoryService.updateStatusAndSync(id, request.getStatus(), request.getNotes());
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update status");
        }
    }

    @PostMapping("/service-history/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> completeServiceHistory(
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        try {
            serviceHistoryService.updateStatusAndSync(id, "COMPLETED", notes);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to complete service");
        }
    }
}
