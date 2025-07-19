package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.service.RequestService;
import com.ambulance.ambulance_service.entity.RequestStatus;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatch")
@CrossOrigin(origins = "*")
public class DispatchController {

    @Autowired
    private RequestService requestService;

    @PostMapping("/{requestId}")
    public ResponseEntity<?> dispatchAmbulance(@PathVariable Long requestId) {
        try {
            requestService.updateRequestStatus(requestId, RequestStatus.DISPATCHED);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Ambulance dispatched successfully");
            return ResponseEntity.ok(response);
        } catch (RequestNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}