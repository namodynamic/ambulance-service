package com.ambulance.ambulance_service.config;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.entity.User;
import com.ambulance.ambulance_service.entity.Patient;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import com.ambulance.ambulance_service.repository.UserRepository;
import com.ambulance.ambulance_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize ambulances
        if (ambulanceRepository.count() == 0) {
            Ambulance amb1 = new Ambulance("Station A - Downtown", AvailabilityStatus.AVAILABLE);
            Ambulance amb2 = new Ambulance("Station B - North Side", AvailabilityStatus.AVAILABLE);
            Ambulance amb3 = new Ambulance("Station C - South District", AvailabilityStatus.MAINTENANCE);
            Ambulance amb4 = new Ambulance("Emergency Station 1", AvailabilityStatus.AVAILABLE);

            ambulanceRepository.save(amb1);
            ambulanceRepository.save(amb2);
            ambulanceRepository.save(amb3);
            ambulanceRepository.save(amb4);

            System.out.println("Initial ambulances created successfully!");
        }

        // Initialize users
        if (userRepository.count() == 0) {
            // Create admin user
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ROLE_ADMIN",
                    "admin@ambulance.com"
            );
            userRepository.save(admin);

            // Create dispatcher user
            User dispatcher = new User(
                    "dispatcher",
                    passwordEncoder.encode("dispatcher123"),
                    "ROLE_DISPATCHER",
                    "dispatcher@ambulance.com"
            );
            userRepository.save(dispatcher);

            // Create regular user
            User user = new User(
                    "user",
                    passwordEncoder.encode("user123"),
                    "ROLE_USER",
                    "user@ambulance.com"
            );
            userRepository.save(user);

            System.out.println("Initial users created successfully!");
            System.out.println("Admin: admin/admin123");
            System.out.println("Dispatcher: dispatcher/dispatcher123");
            System.out.println("User: user/user123");
        }

        // Initialize sample patients
        if (patientRepository.count() == 0) {
            Patient patient1 = new Patient("John Doe", "+1234567890", "No known allergies");
            Patient patient2 = new Patient("Jane Smith", "+1234567891", "Diabetic patient");
            Patient patient3 = new Patient("Bob Johnson", "+1234567892", "Hypertension");

            patientRepository.save(patient1);
            patientRepository.save(patient2);
            patientRepository.save(patient3);

            System.out.println("Initial patients created successfully!");
        }
    }
}