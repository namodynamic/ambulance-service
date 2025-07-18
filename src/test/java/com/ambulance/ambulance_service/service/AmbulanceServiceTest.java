package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmbulanceServiceTest {

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @InjectMocks
    private AmbulanceService ambulanceService;

    private Ambulance ambulance1;
    private Ambulance ambulance2;

    @BeforeEach
    void setUp() {
        // Initialize test data
        ambulance1 = new Ambulance();
        ambulance1.setId(1L);
        ambulance1.setCurrentLocation("Location A");
        ambulance1.setAvailability(true);

        ambulance2 = new Ambulance();
        ambulance2.setId(2L);
        ambulance2.setCurrentLocation("Location B");
        ambulance2.setAvailability(false);
    }

    @Test
    void testGetAllAmbulances() {
        // Arrange
        when(ambulanceRepository.findAll()).thenReturn(Arrays.asList(ambulance1, ambulance2));

        // Act
        List<Ambulance> result = ambulanceService.getAllAmbulances();

        // Assert
        assertEquals(2, result.size());
        verify(ambulanceRepository, times(1)).findAll();
    }

    @Test
    void testGetAvailableAmbulances() {
        // Arrange
        when(ambulanceRepository.findByAvailability(true)).thenReturn(List.of(ambulance1));

        // Act
        List<Ambulance> result = ambulanceService.getAvailableAmbulances();

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).isAvailability());
        verify(ambulanceRepository, times(1)).findByAvailability(true);
    }

    @Test
    void testGetNextAvailableAmbulance() {
        // Arrange
        when(ambulanceRepository.findAll()).thenReturn(Arrays.asList(ambulance1, ambulance2));
        // Call init to load the cache
        ambulanceService.init();

        // Act
        Ambulance result = ambulanceService.getNextAvailableAmbulance();

        // Assert
        assertNotNull(result);
        assertEquals(ambulance1.getId(), result.getId());
        assertTrue(result.isAvailability());
    }

    @Test
    void testMarkAmbulanceUnavailable() {
        // Arrange
        when(ambulanceRepository.findById(1L)).thenReturn(Optional.of(ambulance1));
        when(ambulanceRepository.save(any(Ambulance.class))).thenReturn(ambulance1);
        
        // Initialize with test data
        ambulanceService.init();

        // Act
        ambulanceService.markAmbulanceUnavailable(1L);

        // Assert
        assertFalse(ambulance1.isAvailability());
        verify(ambulanceRepository, times(1)).save(ambulance1);
    }

    @Test
    void testMarkAmbulanceAvailable() {
        // Arrange
        when(ambulanceRepository.findById(2L)).thenReturn(Optional.of(ambulance2));
        when(ambulanceRepository.save(any(Ambulance.class))).thenReturn(ambulance2);
        
        // Initialize with test data
        ambulanceService.init();

        // Act
        ambulanceService.markAmbulanceAvailable(2L);

        // Assert
        assertTrue(ambulance2.isAvailability());
        verify(ambulanceRepository, times(1)).save(ambulance2);
    }
}
