package com.ambulance.ambulance_service.integration;

import com.ambulance.ambulance_service.dto.ServiceHistoryDTO;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.*;
import com.ambulance.ambulance_service.service.*;
import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AmbulanceServiceIntegrationTest {

    @Autowired
    private AmbulanceService ambulanceService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ServiceHistoryService serviceHistoryService;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceHistoryRepository serviceHistoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up database before each test
        serviceHistoryRepository.deleteAll();
        requestRepository.deleteAll();
        patientRepository.deleteAll();
        ambulanceRepository.deleteAll();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Clean up after each test
        serviceHistoryRepository.deleteAll();
        requestRepository.deleteAll();
        patientRepository.deleteAll();
        ambulanceRepository.deleteAll();
    }

    @Test
    void testCompleteEmergencyWorkflow() {
        // Create and save ambulances in a new transaction
        Ambulance ambulance1 = transactionTemplate.execute(status -> {
            Ambulance a1 = new Ambulance();
            a1.setCurrentLocation("Downtown Hospital");
            a1.setAvailability(AvailabilityStatus.AVAILABLE);
            a1.setLicensePlate("AMB001");
            return ambulanceRepository.save(a1);
        });
        
        Ambulance ambulance2 = transactionTemplate.execute(status -> {
            Ambulance a2 = new Ambulance();
            a2.setCurrentLocation("City Medical");
            a2.setAvailability(AvailabilityStatus.AVAILABLE);
            a2.setLicensePlate("AMB002");
            return ambulanceRepository.save(a2);
        });

        // Create a test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        // Create emergency request in a new transaction
        Request createdRequest = transactionTemplate.execute(status -> {
            AmbulanceRequestDto requestDto = new AmbulanceRequestDto(
                "Emergency Patient", "Emergency Patient", "+1234567890", 
                "123 Emergency Street", "Heart attack symptoms", "Test medical notes"
            );
            try {
                return requestService.createRequest(requestDto, testUser);
            } catch (NoAvailableAmbulanceException e) {
                throw new RuntimeException(e);
            }
        });

        assertNotNull(createdRequest, "Request should be created");
        assertEquals(RequestStatus.DISPATCHED, createdRequest.getStatus(), "Request should be dispatched");
        assertNotNull(createdRequest.getAmbulance(), "Ambulance should be assigned");
        assertNotNull(createdRequest.getDispatchTime(), "Dispatch time should be set");

        // Verify ambulance status updated
        Ambulance assignedAmbulance = ambulanceService.getAmbulanceById(createdRequest.getAmbulance().getId()).orElse(null);
        assertNotNull(assignedAmbulance, "Assigned ambulance should exist");
        assertEquals(AvailabilityStatus.DISPATCHED, assignedAmbulance.getAvailability(),
                "Assigned ambulance should be marked as dispatched");

        // Verify patient was created
        Patient createdPatient = (Patient) patientService.findPatientByContact("+1234567890").orElse(null);
        assertNotNull(createdPatient, "Patient should be created");
        assertEquals("Emergency Patient", createdPatient.getName(), "Patient name should match");

        // Verify service history was created
        List<ServiceHistoryDTO> serviceHistories = serviceHistoryService.getAllServiceHistory();
        assertFalse(serviceHistories.isEmpty(), "Service history should be created");

        // Step 3: Verify request was created and ambulance assigned
        assertNotNull(createdRequest, "Request should be created");
        assertEquals(RequestStatus.DISPATCHED, createdRequest.getStatus(), "Request should be dispatched");
        assertNotNull(createdRequest.getAmbulance(), "Ambulance should be assigned");
        assertNotNull(createdRequest.getDispatchTime(), "Dispatch time should be set");

        // Step 4: Verify ambulance status updated
        Ambulance assignedAmbulance2 = ambulanceService.getAmbulanceById(createdRequest.getAmbulance().getId()).orElse(null);
        assertNotNull(assignedAmbulance2, "Assigned ambulance should exist");
        assertEquals(AvailabilityStatus.DISPATCHED, assignedAmbulance2.getAvailability(),
                "Assigned ambulance should be marked as dispatched");

        // Step 5: Verify patient was created
        Patient createdPatient2 = (Patient) patientService.findPatientByContact("+1234567890").orElse(null);
        assertNotNull(createdPatient2, "Patient should be created");
        assertEquals("Emergency Patient", createdPatient2.getName(), "Patient name should match");

        // Step 6: Verify service history was created
        List<ServiceHistoryDTO> serviceHistories2 = serviceHistoryService.getAllServiceHistory();
        assertEquals(1, serviceHistories2.size(), "One service history should be created");

        ServiceHistoryDTO serviceHistory = serviceHistories2.get(0);
        assertEquals(createdRequest.getId(), serviceHistory.getRequestId(), "Service history should link to request");
        assertEquals(createdPatient2.getId(), serviceHistory.getPatientId(), "Service history should link to patient");
        assertEquals(assignedAmbulance2.getId(), serviceHistory.getAmbulanceId(), "Service history should link to ambulance");
        assertEquals(ServiceStatus.IN_PROGRESS, serviceHistory.getStatus(), "Service should be in progress");

        // Update service history to IN_PROGRESS before completing
        serviceHistoryService.updateServiceHistory(
            serviceHistory.getId(), 
            LocalDateTime.now(), 
            null, 
            ServiceStatus.IN_PROGRESS, 
            "Ambulance arrived on scene"
        );

        // Now complete the request
        requestService.updateRequestStatus(createdRequest.getId(), RequestStatus.COMPLETED, "Service completed");

        // Refresh the request to get the latest state
        Request completedRequest = requestService.getRequestById(createdRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.COMPLETED, completedRequest.getStatus(), 
            "Request should be marked as completed");

        // Verify the ambulance is available again
        Ambulance completedAmbulance = ambulanceService.getAmbulanceById(assignedAmbulance.getId()).orElseThrow();
        assertEquals(AvailabilityStatus.AVAILABLE, completedAmbulance.getAvailability(),
                "Ambulance should be available after service completion");

        // Verify service history is properly updated
        List<ServiceHistory> serviceHistories3 = serviceHistoryService.getServiceHistoryByRequestId(createdRequest.getId());
        assertFalse(serviceHistories3.isEmpty(), "Service history should exist");
        
        // Get the most recent service history entry
        ServiceHistory latestHistory = serviceHistories3.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst()
                .orElseThrow();
                
        // First verify the service history is updated to COMPLETED
        if (latestHistory.getStatus() != ServiceStatus.COMPLETED) {
            // If not completed, try to find a COMPLETED entry
            Optional<ServiceHistory> completedHistory = serviceHistories3.stream()
                .filter(sh -> sh.getStatus() == ServiceStatus.COMPLETED)
                .findFirst();
                
            if (completedHistory.isPresent()) {
                latestHistory = completedHistory.get();
            }
        }
        
        assertEquals(ServiceStatus.COMPLETED, latestHistory.getStatus(), 
            "Service should be marked as completed");
        assertNotNull(latestHistory.getCompletionTime(), 
            "Completion time should be recorded when request is completed");
    }


    @Test
    void testMultipleRequests_QueueManagement() {
        // Create and save ambulances in a new transaction
        Ambulance ambulance1 = transactionTemplate.execute(status -> {
            Ambulance a1 = new Ambulance();
            a1.setCurrentLocation("Location 1");
            a1.setAvailability(AvailabilityStatus.AVAILABLE);
            a1.setLicensePlate("AMB001");
            return ambulanceRepository.save(a1);
        });

        Ambulance ambulance2 = transactionTemplate.execute(status -> {
            Ambulance a2 = new Ambulance();
            a2.setCurrentLocation("Location 2");
            a2.setAvailability(AvailabilityStatus.AVAILABLE);
            a2.setLicensePlate("AMB002");
            return ambulanceRepository.save(a2);
        });

        // Verify ambulances are available
        Ambulance savedAmbulance1 = ambulanceRepository.findById(ambulance1.getId())
                .orElseThrow(() -> new AssertionError("Ambulance 1 not found"));
        Ambulance savedAmbulance2 = ambulanceRepository.findById(ambulance2.getId())
                .orElseThrow(() -> new AssertionError("Ambulance 2 not found"));

        assertEquals(AvailabilityStatus.AVAILABLE, savedAmbulance1.getAvailability(),
                "Ambulance 1 should be available");
        assertEquals(AvailabilityStatus.AVAILABLE, savedAmbulance2.getAvailability(),
                "Ambulance 2 should be available");

        // Create a test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        // Test: Create 3 requests (more than available ambulances)
        AmbulanceRequestDto request1 = new AmbulanceRequestDto(
                "Patient 1", "Patient 1", "+1111111111", "Location 1", "Emergency 1", "Notes 1"
        );
        AmbulanceRequestDto request2 = new AmbulanceRequestDto(
                "Patient 2", "Patient 2", "+2222222222", "Location 2", "Emergency 2", "Notes 2"
        );
        AmbulanceRequestDto request3 = new AmbulanceRequestDto(
                "Patient 3", "Patient 3", "+3333333333", "Location 3", "Emergency 3", "Notes 3"
        );

        // First request should be dispatched
        Request createdRequest1 = transactionTemplate.execute(status -> {
            try {
                return requestService.createRequest(request1, testUser);
            } catch (NoAvailableAmbulanceException e) {
                throw new RuntimeException(e);
            }
        });
        assertNotNull(createdRequest1, "First request should be created");
        assertEquals(RequestStatus.DISPATCHED, createdRequest1.getStatus(),
                "First request should be dispatched");
        assertNotNull(createdRequest1.getAmbulance(),
                "First request should have an ambulance assigned");

        // Second request should also be dispatched
        Request createdRequest2 = transactionTemplate.execute(status -> {
            try {
                return requestService.createRequest(request2, testUser);
            } catch (NoAvailableAmbulanceException e) {
                throw new RuntimeException(e);
            }
        });
        assertNotNull(createdRequest2, "Second request should be created");
        assertEquals(RequestStatus.DISPATCHED, createdRequest2.getStatus(),
                "Second request should be dispatched");
        assertNotNull(createdRequest2.getAmbulance(),
                "Second request should have an ambulance assigned");

        // Verify different ambulances were assigned
        assertNotEquals(createdRequest1.getAmbulance().getId(),
                createdRequest2.getAmbulance().getId(),
                "Different ambulances should be assigned to different requests");

        // Verify both ambulances are dispatched
        Ambulance ambulance1AfterDispatch = ambulanceRepository.findById(ambulance1.getId())
            .orElseThrow();
        Ambulance ambulance2AfterDispatch = ambulanceRepository.findById(ambulance2.getId())
            .orElseThrow();
        
        assertEquals(AvailabilityStatus.DISPATCHED, ambulance1AfterDispatch.getAvailability());
        assertEquals(AvailabilityStatus.DISPATCHED, ambulance2AfterDispatch.getAvailability());

        // Third request - no ambulances available, should be queued
        Request queuedRequest = transactionTemplate.execute(status -> {
            try {
                return requestService.createRequest(request3, testUser);
            } catch (NoAvailableAmbulanceException e) {
                throw new RuntimeException(e);
            }
        });

        // Verify the request was created with PENDING status
        assertNotNull(queuedRequest, "Request should be created with PENDING status");
        assertEquals(RequestStatus.PENDING, queuedRequest.getStatus(), 
            "Request should be in PENDING status when no ambulances are available");

        // Verify the service history reflects the PENDING status
        List<ServiceHistory> serviceHistories = serviceHistoryService.getServiceHistoryByRequestId(queuedRequest.getId());
        assertFalse(serviceHistories.isEmpty(), "Service history should exist for the queued request");
        
        // Get the most recent service history entry
        ServiceHistory latestHistory = serviceHistories.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst()
                .orElseThrow();
                
        assertEquals(ServiceStatus.PENDING, latestHistory.getStatus(), 
            "Service history should be in PENDING status");

        // Complete first request to free up an ambulance
        requestService.updateRequestStatus(createdRequest1.getId(), RequestStatus.COMPLETED);

        // Verify ambulance 1 is available again
        Ambulance freedAmbulance = ambulanceService.getAmbulanceById(
                createdRequest1.getAmbulance().getId()
        ).orElseThrow(() -> new AssertionError("Ambulance not found"));
        assertEquals(AvailabilityStatus.AVAILABLE, freedAmbulance.getAvailability(),
                "Ambulance should be available after request completion");

        // Now third request should succeed
        Request createdRequest3 = transactionTemplate.execute(status -> {
            try {
                return requestService.createRequest(request3, testUser);
            } catch (NoAvailableAmbulanceException e) {
                throw new RuntimeException(e);
            }
        });
        assertNotNull(createdRequest3, "Third request should be created after ambulance freed");
        assertEquals(RequestStatus.DISPATCHED, createdRequest3.getStatus(),
                "Third request should be dispatched after ambulance freed");
        assertNotNull(createdRequest3.getAmbulance(),
                "Third request should have an ambulance assigned");

        // Verify the freed ambulance was reused
        assertEquals(freedAmbulance.getId(), createdRequest3.getAmbulance().getId(),
                "Freed ambulance should be reused for the third request");

        // Clean up after test
        serviceHistoryRepository.deleteAll();
        requestRepository.deleteAll();
        patientRepository.deleteAll();
        ambulanceRepository.deleteAll();

    }


    @Test
    void testPatientHistoryTracking() {
        // Create and save ambulance in a new transaction
        Ambulance ambulance = transactionTemplate.execute(status -> {
            Ambulance a = new Ambulance();
            a.setCurrentLocation("Main Hospital");
            a.setAvailability(AvailabilityStatus.AVAILABLE);
            a.setLicensePlate("AMB001");
            return ambulanceRepository.save(a);
        });

        // Create test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        // Create first request in a new transaction
        Request createdRequest1 = transactionTemplate.execute(status -> {
            AmbulanceRequestDto request1 = new AmbulanceRequestDto(
                "John Doe", "John Doe", "+1234567890", 
                "123 Test St", "Chest pain", "History of heart disease"
            );
            try {
                return requestService.createRequest(request1, testUser);
            } catch (NoAvailableAmbulanceException e) {
                throw new RuntimeException(e);
            }
        });
        assertNotNull(createdRequest1, "First request should be created");
        assertEquals(RequestStatus.DISPATCHED, createdRequest1.getStatus(), "First request should be dispatched");
        assertNotNull(createdRequest1.getAmbulance(), "Ambulance should be assigned to first request");

        // Complete first request to free up the ambulance
        requestService.updateRequestStatus(createdRequest1.getId(), RequestStatus.COMPLETED);

        // Verify ambulance is available again
        Ambulance updatedAmbulance = ambulanceService.getAmbulanceById(ambulance.getId())
                .orElseThrow(() -> new AssertionError("Ambulance not found"));
        assertEquals(AvailabilityStatus.AVAILABLE, updatedAmbulance.getAvailability(),
                "Ambulance should be available after request completion");

        // Request 2 - same patient, new request
        AmbulanceRequestDto request2 = new AmbulanceRequestDto(
                "John Doe", "John Doe", "+1234567890", "456 Test Ave", "Difficulty breathing", "Asthma patient"
        );

        Request createdRequest2 = transactionTemplate.execute(status -> {
            try {
                return requestService.createRequest(request2, testUser);
            } catch (NoAvailableAmbulanceException e) {
                throw new RuntimeException(e);
            }
        });
        assertNotNull(createdRequest2, "Second request should be created");
        assertEquals(RequestStatus.DISPATCHED, createdRequest2.getStatus(), "Second request should be dispatched");

        // Verify patient history
        List<Request> patientRequests = requestRepository.findByUserContact("+1234567890");
        assertEquals(2, patientRequests.size(), "Should have two requests for the patient");

        // Verify service history
        List<ServiceHistory> serviceHistories = serviceHistoryRepository.findAll();
        assertEquals(2, serviceHistories.size(), "Should have two service history entries");

        // Verify patient was created only once
        List<Patient> patients = patientRepository.findByContact("+1234567890");
        assertEquals(1, patients.size(), "Should have only one patient record");

        Patient patient = patients.get(0);
        assertTrue(patient.getMedicalNotes().contains("History of heart disease"),
                "Patient's medical history should include first visit notes");
        assertTrue(patient.getMedicalNotes().contains("Asthma patient"),
                "Patient's medical history should include second visit notes");

        // Verify timestamps are in order
        assertTrue(createdRequest1.getRequestTime().isBefore(createdRequest2.getRequestTime()),
                "First request should be before second request");
    }
}
