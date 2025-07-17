package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.service.DispatchService;
import com.ambulance.ambulance_service.exception.ServiceException;
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
    private DispatchService dispatchService;
    
    @PostMapping("/{requestId}")
    public ResponseEntity<?> dispatchAmbulance(@PathVariable Long requestId) {
        try {
            dispatchService.dispatchAmbulance(requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Ambulance dispatched successfully");
            return ResponseEntity.ok(response);
        } catch (ServiceException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}