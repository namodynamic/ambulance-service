package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AmbulanceService {

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    private Map<Long, Ambulance> ambulanceCache = new HashMap<>();
    private Queue<Ambulance> availableQueue = new LinkedList<>();

    @PostConstruct
    public void init() {
        loadAmbulances();
    }

    private void loadAmbulances() {
        List<Ambulance> ambulances = ambulanceRepository.findAll();
        for (Ambulance amb : ambulances) {
            ambulanceCache.put(amb.getId(), amb);
            if (amb.getAvailability() == AvailabilityStatus.AVAILABLE) {
                availableQueue.offer(amb);
            }
        }
    }

    public List<Ambulance> getAllAmbulances() {
        return ambulanceRepository.findAll();
    }

    public Optional<Ambulance> getAmbulanceById(Long id) {
        return ambulanceRepository.findById(id);
    }

    public Ambulance saveAmbulance(Ambulance ambulance) {
        Ambulance saved = ambulanceRepository.save(ambulance);
        ambulanceCache.put(saved.getId(), saved);
        if (saved.getAvailability() == AvailabilityStatus.AVAILABLE) {
            availableQueue.offer(saved);
        }
        return saved;
    }

    public List<Ambulance> getAvailableAmbulances() {
        return ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE);
    }

    public Optional<Ambulance> getNextAvailableAmbulance() {
        return ambulanceRepository.findFirstAvailableAmbulance();
    }

    public void updateAmbulanceStatus(Long ambulanceId, AvailabilityStatus status) {
        Optional<Ambulance> ambulanceOpt = ambulanceRepository.findById(ambulanceId);
        if (ambulanceOpt.isPresent()) {
            Ambulance ambulance = ambulanceOpt.get();
            ambulance.setAvailability(status);
            ambulanceRepository.save(ambulance);

            // Update cache
            ambulanceCache.put(ambulanceId, ambulance);

            // Manage queue
            if (status == AvailabilityStatus.AVAILABLE && !availableQueue.contains(ambulance)) {
                availableQueue.offer(ambulance);
            } else if (status != AvailabilityStatus.AVAILABLE) {
                availableQueue.remove(ambulance);
            }
        }
    }
}