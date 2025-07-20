package com.ambulance.ambulance_service.controller;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.service.AmbulanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AmbulanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AmbulanceService ambulanceService;

    @InjectMocks
    private AmbulanceController ambulanceController;

    private Ambulance ambulance1;
    private Ambulance ambulance2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ambulanceController).build();

        ambulance1 = new Ambulance();
        ambulance1.setId(1L);
        ambulance1.setCurrentLocation("Location A");
        ambulance1.setAvailability(AvailabilityStatus.AVAILABLE);

        ambulance2 = new Ambulance();
        ambulance2.setId(2L);
        ambulance2.setCurrentLocation("Location B");
        ambulance2.setAvailability(AvailabilityStatus.OUT_OF_SERVICE);
    }

    @Test
    void testGetAllAmbulances() throws Exception {
        // Arrange
        when(ambulanceService.getAllAmbulances()).thenReturn(Arrays.asList(ambulance1, ambulance2));

        // Act & Assert
        mockMvc.perform(get("/api/ambulances")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].currentLocation", is("Location A")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].currentLocation", is("Location B")));

        verify(ambulanceService, times(1)).getAllAmbulances();
    }

    @Test
    void testGetAvailableAmbulances() throws Exception {
        // Arrange
        when(ambulanceService.getAvailableAmbulances()).thenReturn(Arrays.asList(ambulance1));

        // Act & Assert
        mockMvc.perform(get("/api/ambulances/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].availability", is("AVAILABLE")));

        verify(ambulanceService, times(1)).getAvailableAmbulances();
    }
}