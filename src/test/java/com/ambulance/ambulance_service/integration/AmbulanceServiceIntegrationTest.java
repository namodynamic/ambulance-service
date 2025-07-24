package com.ambulance.ambulance_service.integration;

import com.ambulance.ambulance_service.dto.ServiceHistoryDTO;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.*;
import com.ambulance.ambulance_service.service.*;
import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
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

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        serviceHistoryRepository.deleteAll();
        requestRepository.deleteAll();
        patientRepository.deleteAll();
        ambulanceRepository.deleteAll();
    }

    @Test
    void testCompleteEmergencyWorkflow() throws NoAvailableAmbulanceException, RequestNotFoundException {
        // Step 1: Create ambulances
        Ambulance ambulance1 = new Ambulance("Downtown Hospital", AvailabilityStatus.AVAILABLE);
        Ambulance ambulance2 = new Ambulance("City Medical", AvailabilityStatus.AVAILABLE);

        ambulance1 = ambulanceService.saveAmbulance(ambulance1);
        ambulance2 = ambulanceService.saveAmbulance(ambulance2);

        // Create a test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        // Step 2: Create emergency request
        AmbulanceRequestDto requestDto = new AmbulanceRequestDto(
                "Emergency Patient",
                "+1234567890",
                "123 Emergency Street",
                "Heart attack symptoms"
        );

        Request createdRequest = requestService.createRequest(requestDto, testUser);

        // Step 3: Verify request was created and ambulance assigned
        assertNotNull(createdRequest, "Request should be created");
        assertEquals(RequestStatus.DISPATCHED, createdRequest.getStatus(), "Request should be dispatched");
        assertNotNull(createdRequest.getAmbulance(), "Ambulance should be assigned");
        assertNotNull(createdRequest.getDispatchTime(), "Dispatch time should be set");

        // Step 4: Verify ambulance status updated
        Ambulance assignedAmbulance = ambulanceService.getAmbulanceById(createdRequest.getAmbulance().getId()).orElse(null);
        assertNotNull(assignedAmbulance, "Assigned ambulance should exist");
        assertEquals(AvailabilityStatus.DISPATCHED, assignedAmbulance.getAvailability(),
                "Assigned ambulance should be marked as dispatched");

        // Step 5: Verify patient was created
        Patient createdPatient = (Patient) patientService.findPatientByContact("+1234567890").orElse(null);
        assertNotNull(createdPatient, "Patient should be created");
        assertEquals("Emergency Patient", createdPatient.getName(), "Patient name should match");

        // Step 6: Verify service history was created
        List<ServiceHistoryDTO> serviceHistories = serviceHistoryService.getAllServiceHistory();
        assertEquals(1, serviceHistories.size(), "One service history should be created");

        ServiceHistoryDTO serviceHistory = serviceHistories.get(0);
        assertEquals(createdRequest.getId(), serviceHistory.getRequestId(), "Service history should link to request");
        assertEquals(createdPatient.getId(), serviceHistory.getPatientId(), "Service history should link to patient");
        assertEquals(assignedAmbulance.getId(), serviceHistory.getAmbulanceId(), "Service history should link to ambulance");
        assertEquals(ServiceStatus.IN_PROGRESS, serviceHistory.getStatus(), "Service should be in progress");

        // Step 7: Update service with arrival time
        LocalDateTime arrivalTime = LocalDateTime.now();
        ServiceHistory entityVersion = serviceHistoryService.getServiceHistoryById(serviceHistory.getId())
                .orElseThrow(() -> new RuntimeException("Service history not found"));

        serviceHistoryService.updateServiceHistory(
                entityVersion.getId(), arrivalTime, null, null, "Ambulance arrived on scene"
        );

        // Step 8: Complete the service
        LocalDateTime completionTime = LocalDateTime.now().plusMinutes(30);
        serviceHistoryService.updateServiceHistory(
                serviceHistory.getId(), null, completionTime, ServiceStatus.COMPLETED, "Patient transported to hospital"
        );

        // Step 9: Complete the request
        requestService.updateRequestStatus(createdRequest.getId(), RequestStatus.COMPLETED);

        // Step 10: Verify ambulance is available again
        Ambulance completedAmbulance = ambulanceService.getAmbulanceById(assignedAmbulance.getId()).orElse(null);
        assertEquals(AvailabilityStatus.AVAILABLE, completedAmbulance.getAvailability(),
                "Ambulance should be available after service completion");

        // Step 11: Verify final state
        Request completedRequest = requestService.getRequestById(createdRequest.getId()).orElse(null);
        assertEquals(RequestStatus.COMPLETED, completedRequest.getStatus(), "Request should be completed");

        ServiceHistoryDTO completedService = serviceHistoryService.getServiceHistoryById(serviceHistory.getId())
                .map(serviceHistoryService::convertToDTO)
                .orElse(null);
        assertNotNull(completedService, "Completed service should exist");
        assertEquals(ServiceStatus.COMPLETED, completedService.getStatus(), "Service should be completed");
        assertNotNull(completedService.getArrivalTime(), "Arrival time should be recorded");
        assertNotNull(completedService.getCompletionTime(), "Completion time should be recorded");
    }

    @Test
    void testMultipleRequests_QueueManagement() throws NoAvailableAmbulanceException, RequestNotFoundException {
        // Setup: Create 2 ambulances
        Ambulance ambulance1 = ambulanceService.saveAmbulance(new Ambulance("Hospital 1", AvailabilityStatus.AVAILABLE));
        Ambulance ambulance2 = ambulanceService.saveAmbulance(new Ambulance("Hospital 2", AvailabilityStatus.AVAILABLE));

        // Create a test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        // Test: Create 3 requests (more than available ambulances)
        AmbulanceRequestDto request1 = new AmbulanceRequestDto("Patient 1", "+1111111111", "Location 1", "Emergency 1");
        AmbulanceRequestDto request2 = new AmbulanceRequestDto("Patient 2", "+2222222222", "Location 2", "Emergency 2");
        AmbulanceRequestDto request3 = new AmbulanceRequestDto("Patient 3", "+3333333333", "Location 3", "Emergency 3");

        // First two requests should be dispatched
        Request createdRequest1 = requestService.createRequest(request1, testUser);
        Request createdRequest2 = requestService.createRequest(request2, testUser);

        // Verify first two requests are dispatched
        assertEquals(RequestStatus.DISPATCHED, createdRequest1.getStatus());
        assertNotNull(createdRequest1.getAmbulance());
        assertEquals(RequestStatus.DISPATCHED, createdRequest2.getStatus());
        assertNotNull(createdRequest2.getAmbulance());

        // Third request should fail - no ambulances available
        assertThrows(NoAvailableAmbulanceException.class, () -> {
            requestService.createRequest(request3, testUser);
        }, "Should throw exception when no ambulances available");

        // Complete first request to free up ambulance
        requestService.updateRequestStatus(createdRequest1.getId(), RequestStatus.COMPLETED);

        // Now third request should succeed
        Request createdRequest3 = requestService.createRequest(request3, testUser);
        assertEquals(RequestStatus.DISPATCHED, createdRequest3.getStatus());
        assertNotNull(createdRequest3.getAmbulance());
    }

    @Test
    void testPatientHistoryTracking() throws NoAvailableAmbulanceException, RequestNotFoundException {
        // Setup ambulance
        ambulanceService.saveAmbulance(new Ambulance("Test Hospital", AvailabilityStatus.AVAILABLE));

        // Create a test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        // Create multiple requests for same patient
        String patientContact = "+1234567890";

        AmbulanceRequestDto request1 = new AmbulanceRequestDto("John Doe", patientContact, "Home", "First emergency");
        AmbulanceRequestDto request2 = new AmbulanceRequestDto("John Doe", patientContact, "Work", "Second emergency");

        Request createdRequest1 = requestService.createRequest(request1, testUser);

        // Complete first request
        requestService.updateRequestStatus(createdRequest1.getId(), RequestStatus.COMPLETED);

        // Add another ambulance for second request
        ambulanceService.saveAmbulance(new Ambulance("Second Hospital", AvailabilityStatus.AVAILABLE));

        Request createdRequest2 = requestService.createRequest(request2, testUser);

        // Verify same patient was used
        assertEquals(createdRequest1.getUserContact(), createdRequest2.getUserContact());

        // Verify patient count (should be only 1 patient created)
        List<Patient> allPatients = patientService.getAllPatients();
        assertEquals(1, allPatients.size(), "Only one patient should exist for same contact");

        Patient patient = allPatients.get(0);
        assertEquals("John Doe", patient.getName());
        assertEquals(patientContact, patient.getContact());

        // Verify service history shows both services for same patient
        List<ServiceHistoryDTO> patientHistory = serviceHistoryRepository.findByPatientContact(patientContact)
                .stream()
                .map(serviceHistoryService::convertToDTO)
                .collect(Collectors.toList());
        assertEquals(2, patientHistory.size(), "Patient should have 2 service history records");
    }
}