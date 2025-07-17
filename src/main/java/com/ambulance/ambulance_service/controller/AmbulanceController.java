package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.service.AmbulanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ambulances")
@CrossOrigin(origins = "*")
public class AmbulanceController {
    
    @Autowired
    private AmbulanceService ambulanceService;
    
    @GetMapping
    public ResponseEntity<?> getAllAmbulances() {
        return ResponseEntity.ok(ambulanceService.getAllAmbulances());
    }
    
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableAmbulances() {
        return ResponseEntity.ok(ambulanceService.getAvailableAmbulances());
    }
}