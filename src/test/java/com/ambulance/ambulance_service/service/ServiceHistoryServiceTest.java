package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.*;
import com.ambulance.ambulance_service.exception.EntityNotFoundException;
import com.ambulance.ambulance_service.repository.ServiceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceHistoryServiceTest {

    @Mock
    private ServiceHistoryRepository serviceHistoryRepository;

    @InjectMocks
    private ServiceHistoryService serviceHistoryService;

    private Request testRequest;
    private Patient testPatient;
    private Ambulance testAmbulance;
    private ServiceHistory testServiceHistory;

    @BeforeEach
    void setUp() {
        // Create test entities
        testRequest = new Request("John Doe", "+1234567890", "Emergency Location", "Chest pain");
        testRequest.setId(1L);
        testRequest.setRequestTime(LocalDateTime.now().minusMinutes(30));

        testPatient = new Patient("John Doe", "+1234567890", "No allergies");
        testPatient.setId(1L);

        testAmbulance = new Ambulance("Downtown Hospital", AvailabilityStatus.DISPATCHED, "AMB123");
        testAmbulance.setId(1L);

        testServiceHistory = new ServiceHistory(testRequest, testPatient, testAmbulance);
        testServiceHistory.setId(1L);
    }

    @Test
    void testCreateServiceHistory_Success() {
        // Arrange
        when(serviceHistoryRepository.save(any(ServiceHistory.class))).thenAnswer(invocation -> {
            ServiceHistory sh = invocation.getArgument(0);
            sh.onCreate();
            sh.setId(1L);
            return sh;
        });

        // Act
        ServiceHistory result = serviceHistoryService.createServiceHistory(testRequest, testPatient, testAmbulance);

        // Assert
        assertNotNull(result, "Service history should be created");
        assertEquals(testRequest, result.getRequest(), "Request should be linked");
        assertEquals(testPatient, result.getPatient(), "Patient should be linked");
        assertEquals(testAmbulance, result.getAmbulance(), "Ambulance should be linked");
        assertEquals(ServiceStatus.IN_PROGRESS, result.getStatus(), "Initial status should be IN_PROGRESS");
        assertNotNull(result.getCreatedAt(), "Created timestamp should be set");

        verify(serviceHistoryRepository, times(1)).save(any(ServiceHistory.class));
    }

    @Test
    void testUpdateServiceHistory_AllFields() {
        // Arrange
        LocalDateTime arrivalTime = LocalDateTime.now().minusMinutes(20);
        LocalDateTime completionTime = LocalDateTime.now().minusMinutes(5);
        String notes = "Patient transported successfully";

        // First set status to ARRIVED to allow transition to COMPLETED
        testServiceHistory.setStatus(ServiceStatus.ARRIVED);

        when(serviceHistoryRepository.findById(1L)).thenReturn(Optional.of(testServiceHistory));
        when(serviceHistoryRepository.save(any(ServiceHistory.class))).thenAnswer(invocation -> {
            ServiceHistory sh = invocation.getArgument(0);
            return sh;
        });

        // Act - Now this is a valid transition from ARRIVED to COMPLETED
        ServiceHistory result = serviceHistoryService.updateServiceHistory(
                1L, arrivalTime, completionTime, ServiceStatus.COMPLETED, notes
        );

        // Assert
        assertNotNull(result, "Updated service history should be returned");
        assertEquals(arrivalTime, result.getArrivalTime(), "Arrival time should be updated");
        assertEquals(completionTime, result.getCompletionTime(), "Completion time should be updated");
        assertEquals(ServiceStatus.COMPLETED, result.getStatus(), "Status should be updated to COMPLETED");
        assertEquals(notes, result.getNotes(), "Notes should be updated");

        verify(serviceHistoryRepository, times(1)).findById(1L);
        verify(serviceHistoryRepository, times(1)).save(testServiceHistory);
    }

    @Test
    void testUpdateServiceHistory_PartialFields() {
        // Arrange - Only update arrival time and notes
        LocalDateTime arrivalTime = LocalDateTime.now().minusMinutes(15);
        String notes = "Ambulance arrived on scene";

        when(serviceHistoryRepository.findById(1L)).thenReturn(Optional.of(testServiceHistory));
        when(serviceHistoryRepository.save(any(ServiceHistory.class))).thenAnswer(invocation -> {
            ServiceHistory sh = invocation.getArgument(0);
            return sh;
        });

        // Act
        ServiceHistory result = serviceHistoryService.updateServiceHistory(
                1L, arrivalTime, null, null, notes
        );

        // Assert
        assertNotNull(result, "Updated service history should be returned");
        assertEquals(arrivalTime, result.getArrivalTime(), "Arrival time should be updated");
        assertNull(result.getCompletionTime(), "Completion time should remain null");
        assertEquals(ServiceStatus.IN_PROGRESS, result.getStatus(), "Status should remain unchanged");
        assertEquals(notes, result.getNotes(), "Notes should be updated");
    }

    @Test
    void testUpdateServiceHistory_NotFound() {
        // Arrange
        when(serviceHistoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            serviceHistoryService.updateServiceHistory(
                    999L, LocalDateTime.now(), null, ServiceStatus.COMPLETED, "Test"
            );
        }, "Should throw EntityNotFoundException for non-existent service history");

        verify(serviceHistoryRepository, times(1)).findById(999L);
        verify(serviceHistoryRepository, never()).save(any());
    }

    @Test
    void testGetServiceHistoryByStatus() {
        // Arrange
        testServiceHistory.setStatus(ServiceStatus.COMPLETED);
        List<ServiceHistory> completedServices = Arrays.asList(testServiceHistory);
        when(serviceHistoryRepository.findByStatus(ServiceStatus.COMPLETED)).thenReturn(completedServices);

        // Act
        List<ServiceHistory> result = serviceHistoryService.getServiceHistoryByStatus(ServiceStatus.COMPLETED);

        // Assert
        assertEquals(1, result.size(), "Should return filtered service history");
        assertEquals(testServiceHistory, result.get(0), "Should return correct service history");
        verify(serviceHistoryRepository, times(1)).findByStatus(ServiceStatus.COMPLETED);
    }

    @Test
    void testGetServiceHistoryByDateRange() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<ServiceHistory> dateRangeServices = Arrays.asList(testServiceHistory);

        when(serviceHistoryRepository.findByCreatedAtBetween(start, end)).thenReturn(dateRangeServices);

        // Act
        List<ServiceHistory> result = serviceHistoryService.getServiceHistoryByDateRange(start, end);

        // Assert
        assertEquals(1, result.size(), "Should return services within date range");
        verify(serviceHistoryRepository, times(1)).findByCreatedAtBetween(start, end);
    }
}