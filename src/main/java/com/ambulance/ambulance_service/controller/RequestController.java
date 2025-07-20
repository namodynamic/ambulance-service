package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.entity.RequestStatus;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> createRequest(@Valid @RequestBody AmbulanceRequestDto requestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        try {
            Request request = requestService.createRequest(requestDto);
            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof NoAvailableAmbulanceException) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", e.getCause().getMessage()));
            }
            throw e;
        } catch (NoAvailableAmbulanceException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Request> updateRequestStatus(@PathVariable Long id,
                                                       @RequestParam RequestStatus status) {
        try {
            Request request = requestService.updateRequestStatus(id, status);
            return ResponseEntity.ok(request);
        } catch (RequestNotFoundException e) {
            return ResponseEntity.notFound().build();
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
}
