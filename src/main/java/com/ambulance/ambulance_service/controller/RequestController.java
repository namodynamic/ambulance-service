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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public List<Request> getAllRequests() {
        return requestService.getAllRequests();
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
                
                // Get the user from the database
                user = userRepository.findByUsername(principal.getUsername())
                    .orElse(null);
                
                if (user != null) {
                    // For authenticated users, use their details from the database
                    // Only set the username from the user object, keep the contact from the request
                    requestDto.setUserName(user.getUsername());
                    // Don't override userContact with email to avoid validation issues
                    // The frontend should provide a valid phone number in the request
                }
            }

            Request request = requestService.createRequest(requestDto, user);
            return ResponseEntity.ok(request);
        } catch (NoAvailableAmbulanceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
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
