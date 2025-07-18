package com.ambulance.ambulance_service.config;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private AmbulanceRepository ambulanceRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if ambulances already exist
        if (ambulanceRepository.count() == 0) {
            // Create initial ambulances
            Ambulance amb1 = new Ambulance("Station A - Downtown", true);
            Ambulance amb2 = new Ambulance("Station B - North Side", true);
            Ambulance amb3 = new Ambulance("Station C - South District", true);
            
            ambulanceRepository.save(amb1);
            ambulanceRepository.save(amb2);
            ambulanceRepository.save(amb3);
            
            System.out.println("Initial ambulances created successfully!");
        }
    }
}