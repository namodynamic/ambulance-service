package com.ambulance.ambulance_service.integration;

import com.ambulance.ambulance_service.AmbulanceServiceApplication;
import com.ambulance.ambulance_service.TestConfig;
import com.ambulance.ambulance_service.dto.ServiceHistoryDTO;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.*;
import com.ambulance.ambulance_service.service.*;
import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AmbulanceServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(TestConfig.class)
@Transactional
class AmbulanceServiceIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(AmbulanceServiceIntegrationTest.class);

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
    private UserRepository userRepository;

    @Autowired
    private RequestStatusHistoryRepository requestStatusHistoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long ambulanceId;
    private User user;

    @BeforeEach
    void setUp() {
        logger.info("Setting up test data...");
        // Clear all data in proper order to respect foreign key constraints
        requestStatusHistoryRepository.deleteAllInBatch();
        serviceHistoryRepository.deleteAllInBatch();
        requestRepository.deleteAllInBatch();
        patientRepository.deleteAllInBatch();
        ambulanceRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        
        // Create test user
        user = new User();
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        // Create test ambulance
        Ambulance ambulance = new Ambulance();
        ambulance.setCurrentLocation("Test Location");
        ambulance.setAvailability(AvailabilityStatus.AVAILABLE);
        ambulance.setLicensePlate("TEST" + System.currentTimeMillis()); // Ensure unique license plate
        ambulance.setModel("Test Model");
        ambulance.setYear(2023);
        ambulance.setCapacity(4);
        ambulance = ambulanceRepository.save(ambulance);
        ambulanceId = ambulance.getId();
        logger.info("Test setup complete");
    }

    @AfterEach
    void tearDown() {
        // Clean up is handled by @Transactional
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testCompleteEmergencyWorkflow() throws NoAvailableAmbulanceException {
        logger.info("Running testCompleteEmergencyWorkflow...");
        // Create and save an ambulance first
        Ambulance ambulance = new Ambulance("Test Hospital", AvailabilityStatus.AVAILABLE, "AMB" + System.currentTimeMillis());
        ambulanceRepository.save(ambulance);
        
        // Create emergency request
        AmbulanceRequestDto requestDto = new AmbulanceRequestDto(
            "Emergency Patient", "Emergency Patient", "+1234567890", 
            "123 Emergency Street", "Heart attack symptoms", "Test medical notes"
        );
        Request createdRequest = requestService.createRequest(requestDto, user);

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testMultipleRequests_QueueManagement() throws NoAvailableAmbulanceException, InterruptedException {
        logger.info("Running testMultipleRequests_QueueManagement...");
        
        // Clean up any existing data first
        requestRepository.deleteAll();
        ambulanceRepository.deleteAll();
        
        // Create exactly 2 ambulances to test queueing behavior
        for (int i = 1; i <= 2; i++) {
            Ambulance ambulance = new Ambulance("Location " + i, AvailabilityStatus.AVAILABLE, 
                "AMB" + System.currentTimeMillis() + i);
            ambulance = ambulanceRepository.save(ambulance);
            logger.info("Created ambulance: {} with status: {}", ambulance.getId(), ambulance.getAvailability());
        }
        
        // Verify we have 2 available ambulances
        List<Ambulance> availableAmbulances = ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE);
        logger.info("Available ambulances: {}", availableAmbulances.size());
        assertEquals(2, availableAmbulances.size(), "Should have 2 available ambulances");

        // Create 3 requests - one more than available ambulances
        List<Request> createdRequests = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            AmbulanceRequestDto requestDto = new AmbulanceRequestDto(
                "User " + i,
                "Patient " + i,
                "+123456789" + i,  // Ensure unique phone number for each request
                "Location " + i,
                "Emergency " + i,
                ""
            );
            
            logger.info("Creating request {}", i);
            Request createdRequest = requestService.createRequest(requestDto, user);
            assertNotNull(createdRequest, "Request should be created");
            createdRequests.add(createdRequest);
            
            // State log after each request
            Request savedRequest = requestRepository.findById(createdRequest.getId()).orElseThrow();
            logger.info("Created request {} with status: {}, ambulance: {}", 
                savedRequest.getId(), 
                savedRequest.getStatus(),
                savedRequest.getAmbulance() != null ? savedRequest.getAmbulance().getId() : "none");
                
            // Small delay between creating requests to ensure proper ordering
            Thread.sleep(200);
        }

        // Get fresh data from the database
        List<Request> allRequests = requestRepository.findAll(Sort.by(Sort.Direction.ASC, "requestTime"));
        assertEquals(3, allRequests.size(), "Three requests should be created");
        
        // Log the state of all requests
        for (int i = 0; i < allRequests.size(); i++) {
            Request r = allRequests.get(i);
            logger.info("Request {}: id={}, status={}, ambulance={}", 
                i, r.getId(), r.getStatus(), 
                r.getAmbulance() != null ? r.getAmbulance().getId() : "none");
        }

        // First two requests should be dispatched
        for (int i = 0; i < 2; i++) {
            Request request = allRequests.get(i);
            request = requestRepository.findById(request.getId()).orElseThrow();
            
            assertEquals(RequestStatus.DISPATCHED, request.getStatus(), 
                String.format("Request %d should be DISPATCHED", i + 1));
            assertNotNull(request.getAmbulance(), 
                String.format("Request %d should have an ambulance assigned", i + 1));
        }

        // Third request should be queued (PENDING)
        Request queuedRequest = allRequests.get(2);
        queuedRequest = requestRepository.findById(queuedRequest.getId()).orElseThrow();
        
        // If this assertion fails, we need to understand why the third request was dispatched
        if (queuedRequest.getStatus() != RequestStatus.PENDING) {
            logger.error("Third request was not queued as expected. Status: {}, Ambulance: {}", 
                queuedRequest.getStatus(),
                queuedRequest.getAmbulance() != null ? queuedRequest.getAmbulance().getId() : "none");
                
            // Check if there are any available ambulances that shouldn't be
            List<Ambulance> ambulances = ambulanceRepository.findAll();
            for (Ambulance a : ambulances) {
                logger.info("Ambulance {}: status={}", a.getId(), a.getAvailability());
            }
        }
        
        assertEquals(RequestStatus.PENDING, queuedRequest.getStatus(), 
            "Third request should be queued (PENDING)");
        assertNull(queuedRequest.getAmbulance(), 
            "Third request should not have an ambulance assigned");

        // Now complete the first request to free up an ambulance
        requestService.updateRequestStatus(
            allRequests.get(0).getId(), 
            RequestStatus.COMPLETED, 
            "First request completed"
        );

        // Manually trigger queue processing
        ((RequestService) requestService).processQueuedRequests();
        
        // Small delay to allow processing to complete
        Thread.sleep(1000);

        // Refresh the queued request from DB
        Request processedRequest = requestRepository.findById(queuedRequest.getId())
            .orElseThrow(() -> new AssertionError("Queued request should exist"));
        
        // The request should now be in progress (not pending)
        assertNotEquals(RequestStatus.PENDING, processedRequest.getStatus(),
            "Queued request should no longer be in PENDING status");
        assertNotNull(processedRequest.getAmbulance(),
            "Queued request should now have an ambulance assigned");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testPatientHistoryTracking() throws NoAvailableAmbulanceException, InterruptedException {
        logger.info("Running testPatientHistoryTracking...");
        
        // Create multiple ambulances first to ensure we have enough capacity
        for (int i = 1; i <= 3; i++) {
            String uniqueLicense = "AMB" + System.currentTimeMillis() + i;
            Ambulance ambulance = new Ambulance("Test Location " + i, AvailabilityStatus.AVAILABLE, uniqueLicense);
            ambulanceRepository.save(ambulance);
        }

        // Create multiple requests for the same patient with the test user
        for (int i = 0; i < 3; i++) {
            AmbulanceRequestDto requestDto = new AmbulanceRequestDto(
                "John Doe",
                "John Doe",
                "+1234567890",  // Same phone number for same patient
                "123 Main St",
                "Emergency " + i,
                ""
            );
            Request createdRequest = requestService.createRequest(requestDto, user);
            assertNotNull(createdRequest, "Request should be created");
            
            // Small delay to ensure proper ordering
            Thread.sleep(100);
        }

        // Allow time for all requests to be processed
        Thread.sleep(2000);
        
        // Get the patient's history
        Optional<Patient> patientOpt = patientRepository.findByContactAndDeletedFalse("+1234567890");
        assertTrue(patientOpt.isPresent(), "Patient should exist");
        
        // Get all requests for this patient
        List<Request> patientRequests = requestRepository.findByUserContact("+1234567890");
        assertEquals(3, patientRequests.size(), "Patient should have three requests in history");
        
        // Verify all requests are dispatched with retry logic
        boolean allDispatched = false;
        int retries = 3;
        
        while (retries-- > 0 && !allDispatched) {
            allDispatched = patientRequests.stream()
                .allMatch(r -> {
                    Request req = requestRepository.findById(r.getId()).orElse(null);
                    return req != null && req.getStatus() == RequestStatus.DISPATCHED;
                });
                
            if (!allDispatched) {
                Thread.sleep(1000); // Wait and retry
                patientRequests = requestRepository.findByUserContact("+1234567890");
            }
        }
        
        assertTrue(allDispatched, "All patient requests should be dispatched");

        // Verify service history for each request
        for (Request request : patientRequests) {
            List<ServiceHistory> histories = serviceHistoryRepository.findByRequestId(request.getId());
            assertFalse(histories.isEmpty(), "Service history should exist for each request");

            // Verify the latest status for each request is IN_PROGRESS
            Optional<ServiceHistory> latestHistory = histories.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .findFirst();

            assertTrue(latestHistory.isPresent(), "Latest history entry should exist");
            
            // Verify status is either IN_PROGRESS or COMPLETED (in case it completed quickly)
            assertTrue(
                latestHistory.get().getStatus() == ServiceStatus.IN_PROGRESS || 
                latestHistory.get().getStatus() == ServiceStatus.COMPLETED,
                "Latest service status should be IN_PROGRESS or COMPLETED"
            );
        }
    }
}
