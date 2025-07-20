package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
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

    private Ambulance availableAmbulance1;
    private Ambulance availableAmbulance2;
    private Ambulance dispatchedAmbulance;
    private Ambulance maintenanceAmbulance;

    @BeforeEach
    void setUp() {
        // Create test ambulances with different statuses
        availableAmbulance1 = new Ambulance();
        availableAmbulance1.setCurrentLocation("Downtown Hospital");
        availableAmbulance1.setAvailability(AvailabilityStatus.AVAILABLE);
        availableAmbulance1.setId(1L);

        availableAmbulance2 = new Ambulance();
        availableAmbulance2.setCurrentLocation("City Medical Center");
        availableAmbulance2.setAvailability(AvailabilityStatus.AVAILABLE);
        availableAmbulance2.setId(2L);

        dispatchedAmbulance = new Ambulance();
        dispatchedAmbulance.setCurrentLocation("Northside Clinic");
        dispatchedAmbulance.setAvailability(AvailabilityStatus.DISPATCHED);
        dispatchedAmbulance.setId(3L);

        maintenanceAmbulance = new Ambulance();
        maintenanceAmbulance.setCurrentLocation("Emergency Station");
        maintenanceAmbulance.setAvailability(AvailabilityStatus.MAINTENANCE);
        maintenanceAmbulance.setId(4L);
    }

    @Test
    void testAmbulanceQueueInitialization() {
        // Arrange
        List<Ambulance> allAmbulances = Arrays.asList(
                availableAmbulance1, availableAmbulance2, dispatchedAmbulance, maintenanceAmbulance
        );
        when(ambulanceRepository.findAll()).thenReturn(allAmbulances);

        // Stub repository calls
        when(ambulanceRepository.findById(1L)).thenReturn(Optional.of(availableAmbulance1));
        when(ambulanceRepository.save(any(Ambulance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - Initialize service
        ambulanceService.init();

        // Act - Get next available ambulance
        Ambulance nextAvailable = ambulanceService.getNextAvailableAmbulance().orElse(null);

        // Assert
        assertNotNull(nextAvailable, "Should have available ambulance in queue");
        assertEquals(AvailabilityStatus.DISPATCHED, nextAvailable.getAvailability(),
                "Next ambulance should be marked as dispatched");

        verify(ambulanceRepository, times(1)).findAll();
        verify(ambulanceRepository, atLeastOnce()).save(any(Ambulance.class));
    }

    @Test
    void testAmbulanceQueueOrder_FIFO() {
        // Arrange
        List<Ambulance> allAmbulances = Arrays.asList(availableAmbulance1, availableAmbulance2);
        when(ambulanceRepository.findAll()).thenReturn(allAmbulances);

        // Stub repository calls
        when(ambulanceRepository.findById(1L)).thenReturn(Optional.of(availableAmbulance1));
        when(ambulanceRepository.findById(2L)).thenReturn(Optional.of(availableAmbulance2));
        when(ambulanceRepository.save(any(Ambulance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ambulanceRepository.findFirstByAvailability(AvailabilityStatus.AVAILABLE))
                .thenReturn(Optional.empty());

        // Act - Initialize service
        ambulanceService.init();

        // Get first ambulance
        Ambulance first = ambulanceService.getNextAvailableAmbulance().orElse(null);
        Ambulance second = ambulanceService.getNextAvailableAmbulance().orElse(null);
        Optional<Ambulance> third = ambulanceService.getNextAvailableAmbulance();

        // Assert - Test FIFO order
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(availableAmbulance1.getId(), first.getId(), "First ambulance should be the first in queue");
        assertEquals(availableAmbulance2.getId(), second.getId(), "Second ambulance should be the next in queue");
        assertTrue(third.isEmpty(), "No more ambulances should be available");

        // Verify status changes
        assertEquals(AvailabilityStatus.DISPATCHED, first.getAvailability());
        assertEquals(AvailabilityStatus.DISPATCHED, second.getAvailability());

        // Verify repository interactions
        verify(ambulanceRepository, times(2)).save(any(Ambulance.class));
    }

    @Test
    void testAmbulanceStatusUpdate_RemovesFromQueue() {
        // Arrange
        when(ambulanceRepository.findAll()).thenReturn(Arrays.asList(availableAmbulance1));
        when(ambulanceRepository.findById(1L)).thenReturn(Optional.of(availableAmbulance1));
        when(ambulanceRepository.save(any(Ambulance.class))).thenReturn(availableAmbulance1);

        // Act
        ambulanceService.init();
        ambulanceService.updateAmbulanceStatus(1L, AvailabilityStatus.DISPATCHED);

        // Assert - Ambulance should be removed from available queue
        assertEquals(AvailabilityStatus.DISPATCHED, availableAmbulance1.getAvailability());
        verify(ambulanceRepository, times(1)).save(availableAmbulance1);
    }

    @Test
    void testAmbulanceStatusUpdate_AddsToQueue() {
        // Arrange
        when(ambulanceRepository.findById(3L)).thenReturn(Optional.of(dispatchedAmbulance));
        when(ambulanceRepository.save(any(Ambulance.class))).thenReturn(dispatchedAmbulance);

        // Act - Change dispatched ambulance back to available
        ambulanceService.updateAmbulanceStatus(3L, AvailabilityStatus.AVAILABLE);

        // Assert
        assertEquals(AvailabilityStatus.AVAILABLE, dispatchedAmbulance.getAvailability());
        verify(ambulanceRepository, times(1)).save(dispatchedAmbulance);
    }

    @Test
    void testGetAvailableAmbulances_FiltersCorrectly() {
        // Arrange
        List<Ambulance> availableOnly = Arrays.asList(availableAmbulance1, availableAmbulance2);
        when(ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE))
                .thenReturn(availableOnly);

        // Act
        List<Ambulance> result = ambulanceService.getAvailableAmbulances();

        // Assert
        assertEquals(2, result.size(), "Should return only available ambulances");
        assertTrue(result.stream().allMatch(a -> a.getAvailability() == AvailabilityStatus.AVAILABLE),
                "All returned ambulances should be available");
        verify(ambulanceRepository, times(1)).findByAvailability(AvailabilityStatus.AVAILABLE);
    }
}