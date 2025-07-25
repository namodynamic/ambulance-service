package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Transactional
public class AmbulanceService implements AmbulanceServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(AmbulanceService.class);

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    private final Map<Long, Ambulance> ambulanceCache = new ConcurrentHashMap<>();
    private final Queue<Ambulance> availableQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        loadAmbulances();
    }

    private synchronized void loadAmbulances() {
        availableQueue.clear();
        ambulanceCache.clear();
        
        List<Ambulance> ambulances = ambulanceRepository.findAll();
        for (Ambulance ambulance : ambulances) {
            ambulanceCache.put(ambulance.getId(), ambulance);
            if (ambulance.getAvailability() == AvailabilityStatus.AVAILABLE) {
                availableQueue.offer(ambulance);
            }
        }
        logger.info("Loaded {} ambulances, {} available", ambulances.size(), availableQueue.size());
    }

    @Override
    public List<Ambulance> getAllAmbulances() {
        return new ArrayList<>(ambulanceCache.values());
    }

    @Override
    public Optional<Ambulance> getAmbulanceById(Long id) {
        return Optional.ofNullable(ambulanceCache.get(id));
    }

    @Override
    public Ambulance saveAmbulance(Ambulance ambulance) {
        Ambulance saved = ambulanceRepository.save(ambulance);
        updateCacheAndQueue(saved);
        return saved;
    }

    @Override
    public void updateAmbulanceStatus(Long ambulanceId, AvailabilityStatus newStatus) {
        ambulanceRepository.findById(ambulanceId).ifPresent(ambulance -> {
            AvailabilityStatus oldStatus = ambulance.getAvailability();
            ambulance.setAvailability(newStatus);
            Ambulance updated = ambulanceRepository.save(ambulance);
            updateCacheAndQueue(updated);
            
            logger.info("Updated ambulance {} status from {} to {}", 
                updated.getId(), oldStatus, newStatus);
        });
    }

    private synchronized void updateCacheAndQueue(Ambulance ambulance) {
        // Update cache
        ambulanceCache.put(ambulance.getId(), ambulance);
        
        // Update queue based on new status
        if (ambulance.getAvailability() == AvailabilityStatus.AVAILABLE) {
            // Only add to queue if not already present
            if (!availableQueue.contains(ambulance)) {
                availableQueue.offer(ambulance);
            }
        } else {
            // Remove from queue if status is not AVAILABLE
            availableQueue.remove(ambulance);
        }
    }

    @Override
    public List<Ambulance> getAvailableAmbulances() {
        return new ArrayList<>(availableQueue);
    }

    @Override
    public Optional<Ambulance> getNextAvailableAmbulance() {
        Ambulance ambulance;
        while ((ambulance = availableQueue.poll()) != null) {
            try {
                // Re-check availability against database
                Optional<Ambulance> currentOpt = ambulanceRepository.findById(ambulance.getId());
                if (currentOpt.isPresent()) {
                    Ambulance current = currentOpt.get();
                    if (current.getAvailability() == AvailabilityStatus.AVAILABLE) {
                        // Update status to DISPATCHED and save
                        current.setAvailability(AvailabilityStatus.DISPATCHED);
                        Ambulance saved = ambulanceRepository.save(current);
                        updateCacheAndQueue(saved);
                        logger.info("Dispatched ambulance: {}", saved.getId());
                        return Optional.of(saved);
                    }
                }
            } catch (ObjectOptimisticLockingFailureException ex) {
                // If there's a conflict, re-add to queue and retry
                if (ambulance.getAvailability() == AvailabilityStatus.AVAILABLE) {
                    availableQueue.offer(ambulance);
                }
                logger.warn("Optimistic lock conflict while dispatching ambulance: {}", ambulance.getId());
                continue;
            }
        }
        logger.warn("No available ambulances found");
        return Optional.empty();
    }

    @Override
    public long countAllAmbulances() {
        return ambulanceCache.size();
    }

    @Override
    public long countAmbulancesByStatus(AvailabilityStatus status) {
        return ambulanceCache.values().stream()
            .filter(a -> a.getAvailability() == status)
            .count();
    }
}