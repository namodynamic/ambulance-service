package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.entity.RequestStatus;
import com.ambulance.ambulance_service.entity.RequestStatusHistory;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.UserRepository;
import com.ambulance.ambulance_service.service.RequestService;
import com.ambulance.ambulance_service.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<Request>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestTime,desc") String[] sort) {

        String sortProperty = (sort.length > 0) ? sort[0] : "requestTime";
        Sort.Direction direction = (sort.length > 1 && sort[1].equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
        
        return ResponseEntity.ok(requestService.getAllRequests(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Request> getRequestById(@PathVariable Long id) {
        Optional<Request> request = requestService.getRequestById(id);
        return request.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRequest(
            @Valid @RequestBody AmbulanceRequestDto requestDto,
            BindingResult bindingResult,
            Authentication authentication) {

        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        try {
            // Get the authenticated user (can be null for unauthenticated requests)
            com.ambulance.ambulance_service.entity.User user = null;
            if (authentication != null && authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User principal = 
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                user = userRepository.findByUsername(principal.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getUsername()));
            }

            Request request = requestService.createRequest(requestDto, user);
            return ResponseEntity.ok(request);
        } catch (NoAvailableAmbulanceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof NoAvailableAmbulanceException) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", cause.getMessage()));
            }
            
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/arrived")
    public ResponseEntity<?> markAmbulanceArrived(
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        try {
            requestService.markAmbulanceArrived(id, notes != null ? notes : "");
            Map<String, String> response = new HashMap<>();
            response.put("message", "Ambulance marked as arrived");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update ambulance status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update the status of a request
     * @param id The ID of the request to update
     * @param statusUpdate Object containing the new status and optional notes
     * @return The updated request
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest statusUpdate) {

        try {
            Request request = requestService.updateRequestStatus(
                id,
                statusUpdate.getStatus(),
                statusUpdate.getNotes()
            );
            return ResponseEntity.ok(request);
        } catch (RequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update request status: " + e.getMessage()));
        }
    }
    
    /**
     * Get the status history for a request
     * @param id The ID of the request
     * @return List of status history entries
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getRequestStatusHistory(@PathVariable Long id) {
        try {
            List<RequestStatusHistory> history = requestService.getRequestStatusHistory(id);
            return ResponseEntity.ok(history);
        } catch (RequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve status history: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status/{status}")
    public List<Request> getRequestsByStatus(@PathVariable RequestStatus status) {
        return requestService.getRequestsByStatus(status);
    }

    @GetMapping("/pending")
    public List<Request> getPendingRequests() {
        return requestService.getPendingRequests();
    }

    @GetMapping("/user/history")
    public ResponseEntity<?> getUserRequestHistory(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            org.springframework.security.core.userdetails.User principal = 
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            
            com.ambulance.ambulance_service.entity.User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Request> userRequests = requestService.getRequestsByUser(user);
            return ResponseEntity.ok(userRequests);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user request history: " + e.getMessage()));
        }
    }

    @GetMapping("/user/active")
    public ResponseEntity<?> getUserActiveRequests(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            org.springframework.security.core.userdetails.User principal = 
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            
            com.ambulance.ambulance_service.entity.User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Request> activeRequests = requestService.getActiveRequestsByUser(user);
            return ResponseEntity.ok(activeRequests);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch active requests: " + e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, String>> handleValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }
    
    /**
     * DTO for status update requests
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        private RequestStatus status;
        private String notes;

        public RequestStatus getStatus() {
            return status;
        }

        public String getNotes() {
            return notes;
        }
    }
}
