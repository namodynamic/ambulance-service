package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.ambulance.ambulance_service.exception.RequestNotFoundException;
import com.ambulance.ambulance_service.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        // Create test data
        validRequestDto = new AmbulanceRequestDto(
                "John Doe",
                "+1234567890",
                "123 Emergency Street",
                "Chest pain"
        );

        initialRequest = new Request(
                validRequestDto.getUserName(),
                validRequestDto.getUserContact(),
                validRequestDto.getLocation(),
                validRequestDto.getEmergencyDescription()
        );
        initialRequest.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        availableAmbulance = new Ambulance();
        availableAmbulance.setCurrentLocation("Downtown Hospital");
        availableAmbulance.setAvailability(AvailabilityStatus.AVAILABLE);
        availableAmbulance.setId(1L);

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

        testPatient = new Patient("John Doe", "+1234567890", "");
        testPatient.setId(1L);
    }

    @Test
    void testCreateRequest_Success() throws NoAvailableAmbulanceException {
        // Arrange
        when(requestRepository.save(any(Request.class)))
                .thenReturn(initialRequest)
                .thenReturn(dispatchedRequest);

        when(ambulanceService.getNextAvailableAmbulance()).thenReturn(Optional.of(availableAmbulance));
        when(patientService.findOrCreatePatient(anyString(), anyString())).thenReturn(testPatient);
        when(serviceHistoryService.createServiceHistory(any(), any(), any())).thenReturn(new ServiceHistory());

        // Act
        Request result = requestService.createRequest(validRequestDto, testUser);

        // Assert
        assertNotNull(result, "Request should be created");
        assertEquals(RequestStatus.DISPATCHED, result.getStatus(), "Request should be dispatched");
        assertNotNull(result.getDispatchTime(), "Dispatch time should be set");
        assertEquals(availableAmbulance, result.getAmbulance(), "Ambulance should be assigned");
        assertEquals(testUser, result.getUser(), "User should be assigned to request");

        // Verify interactions
        verify(requestRepository, times(2)).save(any(Request.class));
        verify(ambulanceService, times(1)).getNextAvailableAmbulance();
        verify(patientService, times(1)).findOrCreatePatient("John Doe", "+1234567890");
        verify(serviceHistoryService, times(1)).createServiceHistory(any(), any(), any());
    }

    @Test
    void testCreateRequest_NoAvailableAmbulance_ThrowsException() {
        // Arrange
        when(requestRepository.save(any(Request.class))).thenReturn(initialRequest);
        when(ambulanceService.getNextAvailableAmbulance()).thenReturn(Optional.empty());

        // Act & Assert
        NoAvailableAmbulanceException exception = assertThrows(
                NoAvailableAmbulanceException.class,
                () -> requestService.createRequest(validRequestDto, testUser),
                "Should throw NoAvailableAmbulanceException when no ambulance available"
        );

        assertEquals("No ambulance available at the moment", exception.getMessage());

        // Verify that request was saved but no further processing occurred
        verify(requestRepository, times(1)).save(any(Request.class));
        verify(ambulanceService, never()).updateAmbulanceStatus(any(), any());
        verify(serviceHistoryService, never()).createServiceHistory(any(), any(), any());
    }

    @Test
    void testUpdateRequestStatus_Success() throws RequestNotFoundException {
        // Arrange
        Request existingRequest = new Request();
        existingRequest.setId(1L);
        existingRequest.setStatus(RequestStatus.DISPATCHED);
        existingRequest.setAmbulance(availableAmbulance);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(existingRequest));
        when(requestRepository.save(any(Request.class))).thenReturn(existingRequest);

        // Act
        Request result = requestService.updateRequestStatus(1L, RequestStatus.COMPLETED);

        // Assert
        assertNotNull(result, "Updated request should be returned");
        assertEquals(RequestStatus.COMPLETED, result.getStatus(), "Status should be updated");

        // Verify ambulance is made available when request is completed
        verify(ambulanceService, times(1)).updateAmbulanceStatus(1L, AvailabilityStatus.AVAILABLE);
        verify(requestRepository, times(1)).save(existingRequest);
    }

    @Test
    void testUpdateRequestStatus_RequestNotFound_ThrowsException() {
        // Arrange
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RequestNotFoundException exception = assertThrows(
                RequestNotFoundException.class,
                () -> requestService.updateRequestStatus(999L, RequestStatus.COMPLETED),
                "Should throw RequestNotFoundException for non-existent request"
        );

        assertEquals("Request not found with id: 999", exception.getMessage());
        verify(requestRepository, never()).save(any());
        verify(ambulanceService, never()).updateAmbulanceStatus(any(), any());
    }

    @Test
    void testValidation_InvalidPhoneNumber() {
        // Arrange - Create DTO with invalid phone number
        AmbulanceRequestDto invalidDto = new AmbulanceRequestDto(
                "John Doe",
                "invalid-phone",
                "123 Emergency Street",
                "Emergency"
        );

        // This test would be handled by @Valid annotation in controller
        // Here we test the pattern manually
        String phonePattern = "^[+]?[0-9]{10,15}$";
        assertFalse(invalidDto.getUserContact().matches(phonePattern),
                "Invalid phone number should not match pattern");
    }
}