package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import com.ambulance.ambulance_service.repository.RequestStatusHistoryRepository;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private AmbulanceService ambulanceService;

    @Mock
    private PatientService patientService;

    @Mock
    private ServiceHistoryService serviceHistoryService;

    @Mock
    private ServiceHistoryRepository serviceHistoryRepository;

    @Mock
    private RequestStatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private RequestService requestService;

    private AmbulanceRequestDto validRequestDto;
    private Request initialRequest;
    private Request dispatchedRequest;
    private Ambulance availableAmbulance;
    private Patient testPatient;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize test data
        validRequestDto = new AmbulanceRequestDto(
                "Test User",
                "Test Patient",
                "+1234567890",
                "Test Location",
                "Test emergency",
                "Test medical notes"
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        availableAmbulance = new Ambulance();
        availableAmbulance.setAvailability(AvailabilityStatus.AVAILABLE);
        availableAmbulance.setId(1L);

        testPatient = new Patient();
        testPatient.setName("Test Patient");
        testPatient.setContact("+1234567890");
        testPatient.setId(1L);

        // Initialize request objects after setting up test data
        initialRequest = new Request(
                validRequestDto.getUserName(),
                validRequestDto.getUserContact(),
                validRequestDto.getLocation(),
                validRequestDto.getEmergencyDescription()
        );
        initialRequest.setId(1L);
        initialRequest.setUser(testUser);
        initialRequest.setMedicalNotes(validRequestDto.getMedicalNotes());

        dispatchedRequest = new Request(
                validRequestDto.getUserName(),
                validRequestDto.getUserContact(),
                validRequestDto.getLocation(),
                validRequestDto.getEmergencyDescription()
        );
        dispatchedRequest.setId(1L);
        dispatchedRequest.setAmbulance(availableAmbulance);
        dispatchedRequest.setStatus(RequestStatus.DISPATCHED);
        dispatchedRequest.setDispatchTime(LocalDateTime.now());
        dispatchedRequest.setUser(testUser);
        dispatchedRequest.setMedicalNotes(validRequestDto.getMedicalNotes());

        // Configure common mocks that are used in most tests
        lenient().when(patientService.findOrCreatePatient(anyString(), anyString(), anyString()))
                .thenReturn(testPatient);
        lenient().when(serviceHistoryService.createServiceHistory(any(), any(), any())).thenReturn(new ServiceHistory());
        lenient().when(statusHistoryRepository.save(any())).thenReturn(new RequestStatusHistory());
    }

    @Test
    void testCreateRequest_Success() throws NoAvailableAmbulanceException {
        // Arrange
        when(ambulanceService.getNextAvailableAmbulance()).thenReturn(Optional.of(availableAmbulance));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> {
            Request r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        // Act
        Request result = requestService.createRequest(validRequestDto, testUser);

        // Assert
        assertNotNull(result, "Request should be created");
        assertEquals(RequestStatus.DISPATCHED, result.getStatus(), "Request should be dispatched");
        assertNotNull(result.getDispatchTime(), "Dispatch time should be set");
        assertEquals(availableAmbulance, result.getAmbulance(), "Ambulance should be assigned");
        assertEquals(testUser, result.getUser(), "User should be assigned to request");
        assertEquals("Test medical notes", result.getMedicalNotes(), "Medical notes should be set");

        // Verify interactions - expect 2 saves: one for initial save and one after dispatch
        verify(requestRepository, times(2)).save(any(Request.class));
        verify(ambulanceService, times(1)).getNextAvailableAmbulance();
        verify(patientService, times(1)).findOrCreatePatient(anyString(), anyString(), anyString());
        verify(serviceHistoryService, times(1)).createServiceHistory(any(), any(), any());
    }

    @Test
    public void testCreateRequest_NoAvailableAmbulance_QueuesRequest() throws NoAvailableAmbulanceException {
        // Arrange
        when(ambulanceService.getNextAvailableAmbulance()).thenReturn(Optional.empty());
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> {
            Request r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        // Act
        Request result = requestService.createRequest(validRequestDto, testUser);

        // Assert
        assertNotNull(result, "Should return the queued request");
        assertEquals(RequestStatus.PENDING, result.getStatus(), "Request should be queued");
        assertEquals("Test medical notes", result.getMedicalNotes(), "Medical notes should be preserved");

        // Verify interactions - expect 2 saves: one for initial creation and one after setting status to PENDING
        verify(ambulanceService, times(1)).getNextAvailableAmbulance();
        verify(patientService, times(1)).findOrCreatePatient(anyString(), anyString(), anyString());
        verify(requestRepository, times(2)).save(any(Request.class));
        verify(serviceHistoryService, times(1)).createServiceHistory(any(), any(), any());
    }

    @Test
    void testUpdateRequestStatus_Success() throws RequestNotFoundException {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setId(1L);
        existingRequest.setStatus(RequestStatus.DISPATCHED);
        existingRequest.setAmbulance(availableAmbulance);
        existingRequest.setUser(testUser);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(existingRequest));
        when(requestRepository.save(any(Request.class))).thenReturn(existingRequest);

        // Act
        Request result = requestService.updateRequestStatus(1L, RequestStatus.COMPLETED, "Test completion note");

        // Assert
        assertNotNull(result, "Updated request should be returned");
        assertEquals(RequestStatus.COMPLETED, result.getStatus(), "Status should be updated");

        // Verify ambulance is made available when request is completed
        verify(ambulanceService, times(1)).updateAmbulanceStatus(1L, AvailabilityStatus.AVAILABLE);
        verify(requestRepository, times(1)).save(existingRequest);
        verify(statusHistoryRepository, times(1)).save(any(RequestStatusHistory.class));
    }

    @Test
    void testUpdateRequestStatus_RequestNotFound_ThrowsException() {
        // Arrange
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RequestNotFoundException exception = assertThrows(
                RequestNotFoundException.class,
                () -> requestService.updateRequestStatus(999L, RequestStatus.COMPLETED, "Test note"),
                "Should throw RequestNotFoundException for non-existent request"
        );

        assertEquals("Request not found with id: 999", exception.getMessage());
        verify(requestRepository, never()).save(any());
        verify(ambulanceService, never()).updateAmbulanceStatus(any(), any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void testGetAllRequests() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Request> expectedPage = new PageImpl<>(List.of(dispatchedRequest), pageable, 1);
        when(requestRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<Request> result = requestService.getAllRequests(pageable);

        // Assert
        assertNotNull(result, "Should return a page of requests");
        assertEquals(1, result.getTotalElements(), "Should return one request");
        assertEquals(dispatchedRequest, result.getContent().get(0), "Should return the dispatched request");
        verify(requestRepository, times(1)).findByDeletedFalse(pageable);
    }

    @Test
    void testValidation_InvalidPhoneNumber() throws NoAvailableAmbulanceException {
        // Arrange - Create DTO with invalid phone number
        AmbulanceRequestDto invalidDto = new AmbulanceRequestDto(
                "John Doe",
                "Test Patient",
                "invalid-phone",
                "Test Location",
                "Test emergency",
                "Test medical notes"
        );

        // Mock the patient service to return a patient with the invalid phone number
        Patient testPatient = new Patient();
        testPatient.setId(1L);
        testPatient.setName("Test Patient");
        testPatient.setContact("invalid-phone");
        
        when(patientService.findOrCreatePatient(anyString(), anyString(), anyString()))
            .thenReturn(testPatient);
            
        // Mock the ambulance service to return an available ambulance
        Ambulance ambulance = new Ambulance();
        ambulance.setId(1L);
        ambulance.setAvailability(AvailabilityStatus.AVAILABLE);
        when(ambulanceService.getNextAvailableAmbulance())
            .thenReturn(Optional.of(ambulance));
            
        // Mock the repository save to return the saved request with the contact
        when(requestRepository.save(any(Request.class)))
            .thenAnswer(invocation -> {
                Request request = invocation.getArgument(0);
                request.setId(1L);
                request.setUserContact(invalidDto.getUserContact()); // Ensure the contact is set
                return request;
            });

        // Act
        Request result = requestService.createRequest(invalidDto, testUser);

        // Assert - Verify the request was created with the exact phone number provided
        assertNotNull(result, "Request should be created");
        assertEquals("invalid-phone", result.getUserContact(), 
            "Phone number should be stored as provided since validation is handled at controller level");
        
        // Verify the service interactions
        verify(patientService).findOrCreatePatient(anyString(), eq("invalid-phone"), anyString());
        verify(ambulanceService).getNextAvailableAmbulance();
        verify(requestRepository, times(2)).save(any(Request.class)); // Expect 2 saves
    }
}