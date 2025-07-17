package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

@Service
public class AmbulanceService {
    
    @Autowired
    private AmbulanceRepository ambulanceRepository;
    
    private Map<Long, Ambulance> ambulanceCache = new HashMap<>();
    private LinkedList<Long> availableQueue = new LinkedList<>();
    
    @jakarta.annotation.PostConstruct
    public void init() {
        loadAmbulances();
    }
    
    private void loadAmbulances() {
        List<Ambulance> ambulances = ambulanceRepository.findAll();
        for (Ambulance amb : ambulances) {
            ambulanceCache.put(amb.getId(), amb);
            if (amb.isAvailability()) {
                availableQueue.add(amb.getId());
            }
        }
    }
    
    public synchronized Ambulance getNextAvailableAmbulance() {
        if (!availableQueue.isEmpty()) {
            Long ambulanceId = availableQueue.poll();
            return ambulanceCache.get(ambulanceId);
        }
        return null;
    }
    
    public void markAmbulanceUnavailable(Long ambulanceId) {
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId).orElse(null);
        if (ambulance != null) {
            ambulance.setAvailability(false);
            ambulanceRepository.save(ambulance);
            ambulanceCache.put(ambulanceId, ambulance);
            availableQueue.remove(ambulanceId);
        }
    }
    
    public void markAmbulanceAvailable(Long ambulanceId) {
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId).orElse(null);
        if (ambulance != null) {
            ambulance.setAvailability(true);
            ambulanceRepository.save(ambulance);
            ambulanceCache.put(ambulanceId, ambulance);
            if (!availableQueue.contains(ambulanceId)) {
                availableQueue.add(ambulanceId);
            }
        }
    }
    
    public List<Ambulance> getAllAmbulances() {
        return ambulanceRepository.findAll();
    }
    
    public List<Ambulance> getAvailableAmbulances() {
        return ambulanceRepository.findByAvailability(true);
    }
}