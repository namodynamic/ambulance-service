package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.service.AmbulanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ambulances")
@CrossOrigin(origins = "*")
public class AmbulanceController {

    @Autowired
    private AmbulanceService ambulanceService;

    @GetMapping
    public List<Ambulance> getAllAmbulances() {
        return ambulanceService.getAllAmbulances();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ambulance> getAmbulanceById(@PathVariable Long id) {
        Optional<Ambulance> ambulance = ambulanceService.getAmbulanceById(id);
        return ambulance.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Ambulance createAmbulance(@RequestBody Ambulance ambulance) {
        return ambulanceService.saveAmbulance(ambulance);
    }

    @GetMapping("/available")
    public List<Ambulance> getAvailableAmbulances() {
        return ambulanceService.getAvailableAmbulances();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateAmbulanceStatus(@PathVariable Long id,
                                                      @RequestParam AvailabilityStatus status) {
        ambulanceService.updateAmbulanceStatus(id, status);
        return ResponseEntity.ok().build();
    }
}