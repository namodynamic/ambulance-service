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

import java.util.*;
import java.util.stream.Collectors;

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
        availableAmbulance1.setLicensePlate("ABC123");

        availableAmbulance2 = new Ambulance();
        availableAmbulance2.setCurrentLocation("City Medical Center");
        availableAmbulance2.setAvailability(AvailabilityStatus.AVAILABLE);
        availableAmbulance2.setId(2L);
        availableAmbulance2.setLicensePlate("DEF456");

        dispatchedAmbulance = new Ambulance();
        dispatchedAmbulance.setCurrentLocation("Northside Clinic");
        dispatchedAmbulance.setAvailability(AvailabilityStatus.DISPATCHED);
        dispatchedAmbulance.setId(3L);
        dispatchedAmbulance.setLicensePlate("GHI789");

        maintenanceAmbulance = new Ambulance();
        maintenanceAmbulance.setCurrentLocation("Emergency Station");
        maintenanceAmbulance.setAvailability(AvailabilityStatus.MAINTENANCE);
        maintenanceAmbulance.setId(4L);
        maintenanceAmbulance.setLicensePlate("JKL012");

        // Common mock behaviors
        lenient().when(ambulanceRepository.save(any(Ambulance.class))).thenAnswer(invocation -> {
            Ambulance a = invocation.getArgument(0);
            return a; // Return the same instance for simplicity
        });
    }

    @Test
    void testAmbulanceQueueInitialization() {
        // Arrange
        List<Ambulance> availableAmbulances = Arrays.asList(availableAmbulance1, availableAmbulance2);
        when(ambulanceRepository.findAll()).thenReturn(availableAmbulances);
        when(ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE))
                .thenReturn(availableAmbulances);

        // Act - Initialize service
        ambulanceService.init();

        // Trigger queue population
        List<Ambulance> available = ambulanceService.getAvailableAmbulances();

        // Assert
        assertEquals(2, available.size(), "Should have loaded all available ambulances");
        assertTrue(available.stream().anyMatch(a -> a.getId().equals(1L)), "Should contain ambulance 1");
        assertTrue(available.stream().anyMatch(a -> a.getId().equals(2L)), "Should contain ambulance 2");

        verify(ambulanceRepository, times(1)).findAll();
        verify(ambulanceRepository, times(1)).findByAvailability(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void testAmbulanceQueueOrder_FIFO() {
        // Arrange
        List<Ambulance> availableAmbulances = Arrays.asList(availableAmbulance1, availableAmbulance2);

        // Mock database responses
        when(ambulanceRepository.findAll()).thenReturn(availableAmbulances);
        when(ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE))
                .thenReturn(availableAmbulances);

        // Mock findByIdWithPessimisticWriteLock to return the ambulances
        when(ambulanceRepository.findByIdWithPessimisticWriteLock(1L))
                .thenReturn(Optional.of(availableAmbulance1));
        when(ambulanceRepository.findByIdWithPessimisticWriteLock(2L))
                .thenReturn(Optional.of(availableAmbulance2));

        // Mock save to update status
        when(ambulanceRepository.save(any(Ambulance.class))).thenAnswer(invocation -> {
            Ambulance a = invocation.getArgument(0);
            if (a.getId().equals(1L)) availableAmbulance1.setAvailability(AvailabilityStatus.DISPATCHED);
            if (a.getId().equals(2L)) availableAmbulance2.setAvailability(AvailabilityStatus.DISPATCHED);
            return a;
        });

        // Initialize the service and load ambulances
        ambulanceService.init();
        ambulanceService.getAvailableAmbulances(); // Trigger queue population

        // Act - Get ambulances in order
        Optional<Ambulance> firstOpt = ambulanceService.getNextAvailableAmbulance();
        Optional<Ambulance> secondOpt = ambulanceService.getNextAvailableAmbulance();
        Optional<Ambulance> thirdOpt = ambulanceService.getNextAvailableAmbulance();

        // Assert - Verify FIFO order
        assertTrue(firstOpt.isPresent(), "First ambulance should be present");
        assertTrue(secondOpt.isPresent(), "Second ambulance should be present");
        assertTrue(thirdOpt.isEmpty(), "No third ambulance should be available");

        // Verify the order is correct (FIFO)
        assertEquals(availableAmbulance1.getId(), firstOpt.get().getId(),
                "First ambulance should be the first one in the queue");
        assertEquals(availableAmbulance2.getId(), secondOpt.get().getId(),
                "Second ambulance should be the second one in the queue");
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
        List<Ambulance> allAmbulances = Arrays.asList(
                availableAmbulance1,
                availableAmbulance2,
                dispatchedAmbulance,
                maintenanceAmbulance
        );

        when(ambulanceRepository.findAll()).thenReturn(allAmbulances);
        when(ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE))
                .thenReturn(Arrays.asList(availableAmbulance1, availableAmbulance2));

        // Initialize the service to load ambulances
        ambulanceService.init();

        // Act
        List<Ambulance> result = ambulanceService.getAvailableAmbulances();

        // Assert
        assertEquals(2, result.size(), "Should return only available ambulances");

        // Verify only available ambulances are returned
        assertTrue(result.stream().allMatch(a -> a.getAvailability() == AvailabilityStatus.AVAILABLE),
                "All returned ambulances should be available");

        // Verify the specific ambulances returned
        Set<Long> resultIds = result.stream().map(Ambulance::getId).collect(Collectors.toSet());
        assertTrue(resultIds.contains(availableAmbulance1.getId()), "Should contain first available ambulance");
        assertTrue(resultIds.contains(availableAmbulance2.getId()), "Should contain second available ambulance");
        assertFalse(resultIds.contains(dispatchedAmbulance.getId()), "Should not contain dispatched ambulance");
        assertFalse(resultIds.contains(maintenanceAmbulance.getId()), "Should not contain ambulance in maintenance");

        // Verify repository interactions
        verify(ambulanceRepository, atLeastOnce()).findByAvailability(AvailabilityStatus.AVAILABLE);
    }
}