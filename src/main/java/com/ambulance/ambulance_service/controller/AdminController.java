package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.dto.AdminDashboardStats;
import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.entity.Patient;
import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.service.AmbulanceService;
import com.ambulance.ambulance_service.service.PatientService;
import com.ambulance.ambulance_service.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RequestService requestService;
    private final AmbulanceService ambulanceService;
    private final PatientService patientService;

    @Autowired
    public AdminController(RequestService requestService,
                          AmbulanceService ambulanceService,
                          PatientService patientService) {
        this.requestService = requestService;
        this.ambulanceService = ambulanceService;
        this.patientService = patientService;
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
    public ResponseEntity<List<Request>> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }

    @GetMapping("/ambulances")
    public ResponseEntity<List<Ambulance>> getAllAmbulances() {
        return ResponseEntity.ok(ambulanceService.getAllAmbulances());
    }

    @GetMapping("/patients")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }
}
