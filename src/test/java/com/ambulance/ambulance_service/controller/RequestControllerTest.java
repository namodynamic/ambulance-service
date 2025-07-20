package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.dto.AmbulanceRequestDto;
import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.service.RequestService;
import com.ambulance.ambulance_service.exception.NoAvailableAmbulanceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestController requestController;

    private AmbulanceRequestDto validRequestDto;
    private Request mockRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For Java 8 date/time support
        mockMvc = MockMvcBuilders.standaloneSetup(requestController).build();

        validRequestDto = new AmbulanceRequestDto(
                "John Doe",
                "+1234567890",
                "123 Emergency Street",
                "Chest pain"
        );

        mockRequest = new Request(
                validRequestDto.getUserName(),
                validRequestDto.getUserContact(),
                validRequestDto.getLocation(),
                validRequestDto.getEmergencyDescription()
        );
        mockRequest.setId(1L);
        mockRequest.setStatus(RequestStatus.DISPATCHED);
        mockRequest.setRequestTime(LocalDateTime.now());
        mockRequest.setDispatchTime(LocalDateTime.now());
    }

    @Test
    void testCreateRequest_Success() throws Exception {
        // Arrange
        when(requestService.createRequest(any(AmbulanceRequestDto.class))).thenReturn(mockRequest);

        // Act & Assert
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userName", is("John Doe")))
                .andExpect(jsonPath("$.userContact", is("+1234567890")))
                .andExpect(jsonPath("$.location", is("123 Emergency Street")))
                .andExpect(jsonPath("$.status", is("DISPATCHED")))
                .andExpect(jsonPath("$.requestTime", notNullValue()))
                .andExpect(jsonPath("$.dispatchTime", notNullValue()));

        verify(requestService, times(1)).createRequest(any(AmbulanceRequestDto.class));
    }

    @Test
    void testCreateRequest_ValidationError_InvalidPhone() throws Exception {
        // Arrange - Invalid phone number
        AmbulanceRequestDto invalidDto = new AmbulanceRequestDto(
                "John Doe",
                "invalid-phone",
                "123 Emergency Street",
                "Emergency"
        );

        // Act & Assert
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userContact", containsString("Invalid phone number format")));

        verify(requestService, never()).createRequest((AmbulanceRequestDto) any());
    }

    @Test
    void testCreateRequest_ValidationError_MissingFields() throws Exception {
        // Arrange - Missing required fields
        AmbulanceRequestDto invalidDto = new AmbulanceRequestDto();
        invalidDto.setEmergencyDescription("Emergency"); // Only set optional field

        // Act & Assert
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userName", containsString("required")))
                .andExpect(jsonPath("$.userContact", containsString("required")))
                .andExpect(jsonPath("$.location", containsString("required")));

        verify(requestService, never()).createRequest((AmbulanceRequestDto) any());
    }

    @Test
    void testCreateRequest_NoAvailableAmbulance() throws Exception {
        // Arrange
        when(requestService.createRequest(any(AmbulanceRequestDto.class)))
                .thenThrow(new RuntimeException(new NoAvailableAmbulanceException("No ambulance available")));

        // Act & Assert - Changed from isInternalServerError() to isServiceUnavailable()
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isServiceUnavailable()) // Updated to 503
                .andExpect(jsonPath("$.error", is("No ambulance available"))); // Added response body check

        verify(requestService, times(1)).createRequest(any(AmbulanceRequestDto.class));
    }

    @Test
    void testGetAllRequests() throws Exception {
        // Arrange
        Request request1 = new Request("Patient 1", "+1111111111", "Location 1", "Emergency 1");
        request1.setId(1L);
        request1.setStatus(RequestStatus.PENDING);

        Request request2 = new Request("Patient 2", "+2222222222", "Location 2", "Emergency 2");
        request2.setId(2L);
        request2.setStatus(RequestStatus.DISPATCHED);

        when(requestService.getAllRequests()).thenReturn(Arrays.asList(request1, request2));

        // Act & Assert
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].userName", is("Patient 1")))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].userName", is("Patient 2")))
                .andExpect(jsonPath("$[1].status", is("DISPATCHED")));

        verify(requestService, times(1)).getAllRequests();
    }

    @Test
    void testGetRequestById_Found() throws Exception {
        // Arrange
        when(requestService.getRequestById(1L)).thenReturn(Optional.of(mockRequest));

        // Act & Assert
        mockMvc.perform(get("/api/requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userName", is("John Doe")));

        verify(requestService, times(1)).getRequestById(1L);
    }

    @Test
    void testGetRequestById_NotFound() throws Exception {
        // Arrange
        when(requestService.getRequestById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/requests/999"))
                .andExpect(status().isNotFound());

        verify(requestService, times(1)).getRequestById(999L);
    }

    @Test
    void testUpdateRequestStatus() throws Exception {
        // Arrange
        Request updatedRequest = new Request();
        updatedRequest.setId(1L);
        updatedRequest.setStatus(RequestStatus.COMPLETED);

        when(requestService.updateRequestStatus(1L, RequestStatus.COMPLETED))
                .thenReturn(updatedRequest);

        // Act & Assert
        mockMvc.perform(put("/api/requests/1/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(requestService, times(1)).updateRequestStatus(1L, RequestStatus.COMPLETED);
    }

    @Test
    void testGetPendingRequests() throws Exception {
        // Arrange
        Request pendingRequest = new Request("Pending Patient", "+5555555555", "Location", "Emergency");
        pendingRequest.setId(1L);
        pendingRequest.setStatus(RequestStatus.PENDING);

        when(requestService.getPendingRequests()).thenReturn(Arrays.asList(pendingRequest));

        // Act & Assert
        mockMvc.perform(get("/api/requests/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        verify(requestService, times(1)).getPendingRequests();
    }
}