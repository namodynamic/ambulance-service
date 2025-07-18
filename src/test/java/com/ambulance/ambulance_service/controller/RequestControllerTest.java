package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.dto.RequestDTO;
import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.Request;
import com.ambulance.ambulance_service.service.RequestService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class RequestControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestController requestController;

    private Request request1;
    private Request request2;
    private RequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(requestController).build();
        objectMapper = new ObjectMapper();

        // Configure ObjectMapper to handle Java 8 date/time types
        objectMapper.findAndRegisterModules();

        // Create test data
        request1 = new Request();
        request1.setId(1L);
        request1.setUserName("John Doe");
        request1.setUserContact("1234567890");
        request1.setLocation("123 Main St");
        request1.setRequestTime(LocalDateTime.now());
        request1.setStatus("PENDING");

        request2 = new Request();
        request2.setId(2L);
        request2.setUserName("Jane Smith");
        request2.setUserContact("0987654321");
        request2.setLocation("456 Oak Ave");
        request2.setRequestTime(LocalDateTime.now().minusHours(1));
        request2.setStatus("COMPLETED");

        requestDTO = new RequestDTO();
        requestDTO.setUserName("John Doe");
        requestDTO.setUserContact("1234567890");
        requestDTO.setLocation("123 Main St");
    }

    @Test
    void testCreateRequest() throws Exception {
        // Arrange
        when(requestService.createRequest(any(Request.class))).thenReturn(request1);

        // Act & Assert
        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userName", is("John Doe")))
                .andExpect(jsonPath("$.status", is("PENDING")));

        verify(requestService, times(1)).createRequest(any(Request.class));
    }

    @Test
    void testGetAllRequests() throws Exception {
        // Arrange
        when(requestService.getAllRequests()).thenReturn(Arrays.asList(request1, request2));

        // Act & Assert
        mockMvc.perform(get("/api/requests")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userName", is("John Doe")))
                .andExpect(jsonPath("$[1].userName", is("Jane Smith")));

        verify(requestService, times(1)).getAllRequests();
    }

    @Test
    void testGetPendingRequests() throws Exception {
        // Arrange
        when(requestService.getPendingRequests()).thenReturn(Arrays.asList(request1));

        // Act & Assert
        mockMvc.perform(get("/api/requests/pending")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        verify(requestService, times(1)).getPendingRequests();
    }
}
