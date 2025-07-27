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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private User mockUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For Java 8 date/time support
        mockMvc = MockMvcBuilders.standaloneSetup(requestController).build();

        validRequestDto = new AmbulanceRequestDto(
                "John Doe",  // userName
                "Jane Smith", // patientName
                "+1234567890", // userContact
                "123 Emergency Street", // location
                "Chest pain: Patient has a history of heart disease", // emergencyDescription
                "" // medicalNotes
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

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setRole(Role.USER);

    }

    @Test
    void testCreateRequest_Success() throws Exception {
        // Arrange
        when(requestService.createRequest(any(AmbulanceRequestDto.class), any())).thenReturn(mockRequest);

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

        verify(requestService, times(1)).createRequest(any(AmbulanceRequestDto.class), any());
    }

    @Test
    void testCreateRequest_ValidationError_InvalidPhone() throws Exception {
        // Arrange - Invalid phone number
        AmbulanceRequestDto invalidDto = new AmbulanceRequestDto(
                "John Doe",
                "Jane Smith",
                "invalid-phone",
                "123 Emergency Street",
                "Emergency",
                "Patient has a history of heart disease"
        );

        // Act & Assert
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userContact", containsString("Invalid phone number format")));

        verify(requestService, never()).createRequest(any(AmbulanceRequestDto.class), any());
    }

    @Test
    void testCreateRequest_ValidationError_MissingFields() throws Exception {
        // Arrange - Missing required fields
        AmbulanceRequestDto invalidDto = new AmbulanceRequestDto();
        invalidDto.setEmergencyDescription("Emergency"); // Only set optional field
        invalidDto.setMedicalNotes("Patient has a history of heart disease"); // Set medical notes

        // Act & Assert
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userName", containsString("required")))
                .andExpect(jsonPath("$.userContact", containsString("required")))
                .andExpect(jsonPath("$.location", containsString("required")));

        verify(requestService, never()).createRequest(any(AmbulanceRequestDto.class), any());
    }

    @Test
    void testCreateRequest_NoAvailableAmbulance() throws Exception {
        when(requestService.createRequest(any(AmbulanceRequestDto.class), any()))
                .thenThrow(new RuntimeException(new NoAvailableAmbulanceException("No ambulance available")));

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", is("No ambulance available")));

        verify(requestService, times(1)).createRequest(any(AmbulanceRequestDto.class), any());
    }

    @Test
    void testGetAllRequests() throws Exception {
        // Arrange
        Request request1 = new Request("Patient 1", "+1111111111", "Location 1", "Emergency 1");
        request1.setId(1L);
        request1.setStatus(RequestStatus.PENDING);
        request1.setRequestTime(LocalDateTime.now());

        Request request2 = new Request("Patient 2", "+2222222222", "Location 2", "Emergency 2");
        request2.setId(2L);
        request2.setStatus(RequestStatus.DISPATCHED);
        request2.setRequestTime(LocalDateTime.now().minusHours(1));

        // Create a page of requests with proper sorting
        Pageable pageable = PageRequest.of(0, 10, Sort.by("requestTime").descending());
        Page<Request> page = new PageImpl<>(Arrays.asList(request1, request2), pageable, 2);
        
        // Mock the service call with any Pageable
        when(requestService.getAllRequests(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/requests")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "requestTime,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].userName", is("Patient 1")))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].userName", is("Patient 2")))
                .andExpect(jsonPath("$.content[1].status", is("DISPATCHED")));

        verify(requestService, times(1)).getAllRequests(any(Pageable.class));
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

        // Mock the service call with all parameters including the notes
        when(requestService.updateRequestStatus(1L, RequestStatus.COMPLETED, null))
                .thenReturn(updatedRequest);

        // Create a StatusUpdateRequest object to send in the request body
        RequestController.StatusUpdateRequest statusUpdate = new RequestController.StatusUpdateRequest();
        statusUpdate.setStatus(RequestStatus.COMPLETED);
        statusUpdate.setNotes(null);

        // Act & Assert
        mockMvc.perform(put("/api/requests/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Verify the service was called with the correct parameters
        verify(requestService, times(1))
                .updateRequestStatus(1L, RequestStatus.COMPLETED, null);
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