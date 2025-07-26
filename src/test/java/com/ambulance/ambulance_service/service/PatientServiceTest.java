package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Patient;
import com.ambulance.ambulance_service.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        testPatient = new Patient("Jane Doe", "+1987654321", "Diabetic patient");
        testPatient.setId(1L);
    }

    @Test
    void testGenericGetPatientRecord_WithPatientClass() {
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));

        // Act - Test generic method with Patient class
        Optional<Patient> result = patientService.getPatientRecord(1L, Patient.class);

        // Assert
        assertTrue(result.isPresent(), "Should return patient when found");
        assertEquals(testPatient, result.get(), "Should return correct patient");
        assertEquals("Jane Doe", result.get().getName(), "Patient name should match");
        verify(patientRepository, times(1)).findById(1L);
    }

    @Test
    void testGenericGetPatientRecord_WithInvalidClass() {
        // Act - Test generic method with invalid class type
        Optional<String> result = patientService.getPatientRecord(1L, String.class);

        // Assert
        assertFalse(result.isPresent(), "Should return empty for invalid class type");
        verify(patientRepository, never()).findById(any());
    }

    @Test
    void testGenericGetPatientRecord_NotFound() {
        // Arrange
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Patient> result = patientService.getPatientRecord(999L, Patient.class);

        // Assert
        assertFalse(result.isPresent(), "Should return empty when patient not found");
        verify(patientRepository, times(1)).findById(999L);
    }

    @Test
    void testGenericSavePatientRecord() {
        // Arrange
        Patient newPatient = new Patient("Bob Smith", "+1122334455", "No allergies");
        when(patientRepository.save(any(Patient.class))).thenReturn(newPatient);

        // Act - Test generic save method
        Patient result = patientService.savePatientRecord(newPatient);

        // Assert
        assertNotNull(result, "Should return saved patient");
        assertEquals(newPatient, result, "Should return the same patient instance");
        assertEquals("Bob Smith", result.getName(), "Patient name should be preserved");
        verify(patientRepository, times(1)).save(newPatient);
    }

    @Test
    void testFindOrCreatePatient_ExistingPatient() {
        // Arrange - Patient already exists
        when(patientRepository.findByContact("+1987654321")).thenReturn(List.of(testPatient));

        // Act
        Patient result = patientService.findOrCreatePatient("Jane Doe", "+1987654321");

        // Assert
        assertEquals(testPatient, result, "Should return existing patient");
        verify(patientRepository, times(1)).findByContact("+1987654321");
        verify(patientRepository, never()).save(any()); // Should not save new patient
    }

    @Test
    void testFindOrCreatePatient_NewPatient() {
        // Arrange - Patient doesn't exist
        when(patientRepository.findByContact("+1555666777")).thenReturn(Collections.emptyList());

        Patient newPatient = new Patient("New Patient", "+1555666777", "");
        when(patientRepository.save(any(Patient.class))).thenReturn(newPatient);

        // Act
        Patient result = patientService.findOrCreatePatient("New Patient", "+1555666777");

        // Assert
        assertNotNull(result, "Should create and return new patient");
        assertEquals("New Patient", result.getName(), "New patient name should match");
        assertEquals("+1555666777", result.getContact(), "New patient contact should match");
        assertEquals("", result.getMedicalNotes(), "New patient should have empty medical notes");

        verify(patientRepository, times(1)).findByContact("+1555666777");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    void testGenericMethodsWithInheritance() {
        // Create a subclass of Patient for testing
        class ExtendedPatient extends Patient {
            private String additionalInfo;

            public ExtendedPatient(String name, String contact, String medicalNotes, String additionalInfo) {
                super(name, contact, medicalNotes);
                this.additionalInfo = additionalInfo;
            }

            public String getAdditionalInfo() { return additionalInfo; }
        }

        // Arrange
        ExtendedPatient extendedPatient = new ExtendedPatient("Extended Patient", "+1999888777", "Test notes", "Extra info");
        when(patientRepository.save(any(Patient.class))).thenReturn(extendedPatient);

        // Act - Test generic method with subclass
        ExtendedPatient result = (ExtendedPatient) patientService.savePatientRecord(extendedPatient);

        // Assert
        assertNotNull(result, "Should handle subclasses correctly");
        assertEquals("Extended Patient", result.getName(), "Should preserve base class properties");
        assertEquals("Extra info", result.getAdditionalInfo(), "Should preserve subclass properties");
        verify(patientRepository, times(1)).save(extendedPatient);
    }
}