package com.ambulance.ambulance_service.config;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.Patient;
import com.ambulance.ambulance_service.entity.Role;
import com.ambulance.ambulance_service.entity.User;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import com.ambulance.ambulance_service.repository.PatientRepository;
import com.ambulance.ambulance_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * DataInitializer populates the database with initial test data.
 * This will only run in the 'dev' or 'test' profiles.
 */
@Component
@Profile({"dev", "test"})
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                          AmbulanceRepository ambulanceRepository,
                          PatientRepository patientRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Only initialize if no users exist
        if (userRepository.count() == 0) {
            createAdminUser();
            createDispatcherUser();
            createRegularUser();
            createAmbulances();
            createPatients();
        }
    }

    private void createAdminUser() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin.setEmail("admin@ambulance.com");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEnabled(true);
        userRepository.save(admin);
    }

    private void createDispatcherUser() {
        User dispatcher = new User();
        dispatcher.setUsername("dispatcher");
        dispatcher.setPassword(passwordEncoder.encode("dispatcher123"));
        dispatcher.setRole(Role.DISPATCHER);
        dispatcher.setEmail("dispatcher@ambulance.com");
        dispatcher.setFirstName("Dispatch");
        dispatcher.setLastName("Officer");
        dispatcher.setEnabled(true);
        userRepository.save(dispatcher);
    }

    private void createRegularUser() {
        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setRole(Role.USER);
        user.setEmail("user@ambulance.com");
        user.setFirstName("Regular");
        user.setLastName("User");
        user.setEnabled(true);
        userRepository.save(user);
    }

    private void createAmbulances() {
        for (int i = 1; i <= 5; i++) {
            Ambulance ambulance = new Ambulance();
            ambulance.setCurrentLocation("Location " + i);
            ambulance.setAvailability(AvailabilityStatus.AVAILABLE);
            ambulanceRepository.save(ambulance);
        }
    }

    private void createPatients() {
        Patient patient1 = new Patient();
        patient1.setName("John Doe");
        patient1.setContact("+2340567890");
        patient1.setMedicalNotes("Allergic to penicillin");
        patient1.setCreatedAt(LocalDateTime.now());
        patient1.setUpdatedAt(LocalDateTime.now());
        patientRepository.save(patient1);

        Patient patient2 = new Patient();
        patient2.setName("Jane Smith");
        patient2.setContact("+23407654321");
        patient2.setMedicalNotes("History of heart conditions");
        patient2.setCreatedAt(LocalDateTime.now());
        patient2.setUpdatedAt(LocalDateTime.now());
        patientRepository.save(patient2);
    }
}