package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

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
        try {
            loadAmbulances();
        } catch (Exception e) {
            logger.error("Failed to initialize ambulance service: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize ambulance service", e);
        }
    }

    private synchronized void loadAmbulances() {
        try {
            logger.info("Loading ambulances from database...");
            
            // Clear existing state
            availableQueue.clear();
            ambulanceCache.clear();

            // Load all ambulances from the database
            List<Ambulance> allAmbulances = ambulanceRepository.findAll();
            logger.info("Found {} total ambulances in database", allAmbulances.size());

            // Process each ambulance
            for (Ambulance ambulance : allAmbulances) {
                // Create a defensive copy to avoid modifying the original object
                Ambulance cachedAmbulance = new Ambulance();
                // Copy all properties from the original ambulance
                cachedAmbulance.setId(ambulance.getId());
                cachedAmbulance.setCurrentLocation(ambulance.getCurrentLocation());
                cachedAmbulance.setAvailability(ambulance.getAvailability());
                cachedAmbulance.setLicensePlate(ambulance.getLicensePlate());
                // Copy any other fields as needed
                
                ambulanceCache.put(cachedAmbulance.getId(), cachedAmbulance);
                
                // Only add available ambulances to the queue
                if (cachedAmbulance.getAvailability() == AvailabilityStatus.AVAILABLE) {
                    availableQueue.offer(cachedAmbulance);
                    logger.debug("Added available ambulance {} to queue", cachedAmbulance.getId());
                }
            }

            logger.info("Loaded {} total ambulances, {} available",
                    allAmbulances.size(), availableQueue.size());
                    
        } catch (Exception e) {
            logger.error("Error loading ambulances: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to load ambulances", e);
        }
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
        logger.info("Saved ambulance ID: {}, Status: {}", saved.getId(), saved.getAvailability());
        return saved;
    }

    @Override
    public void updateAmbulanceStatus(Long ambulanceId, AvailabilityStatus newStatus) {
        ambulanceRepository.findById(ambulanceId).ifPresent(ambulance -> {
            AvailabilityStatus oldStatus = ambulance.getAvailability();
            if (oldStatus != newStatus) {
                ambulance.setAvailability(newStatus);
                Ambulance updated = ambulanceRepository.save(ambulance);
                updateCacheAndQueue(updated);
                logger.info("Updated ambulance {} status from {} to {}",
                        updated.getId(), oldStatus, newStatus);
            }
        });
    }

    private synchronized void updateCacheAndQueue(Ambulance ambulance) {
        if (ambulance == null) return;

        // Update cache
        ambulanceCache.put(ambulance.getId(), ambulance);

        // Update queue based on new status
        if (ambulance.getAvailability() == AvailabilityStatus.AVAILABLE) {
            // Remove if exists to avoid duplicates, then add to end
            availableQueue.removeIf(a -> a.getId().equals(ambulance.getId()));
            availableQueue.offer(ambulance);
            logger.debug("Added/Updated ambulance {} in available queue", ambulance.getId());
        } else {
            boolean removed = availableQueue.removeIf(a -> a.getId().equals(ambulance.getId()));
            if (removed) {
                logger.debug("Removed ambulance {} from available queue (new status: {})",
                        ambulance.getId(), ambulance.getAvailability());
            }
        }
    }

    @Override
    public List<Ambulance> getAvailableAmbulances() {
        // Get fresh data from database
        List<Ambulance> available = ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE);
        
        // Update cache and queue
        synchronized (this) {
            // Update cache
            available.forEach(ambulance -> {
                Ambulance cached = ambulanceCache.computeIfAbsent(ambulance.getId(), id -> ambulance);
                // Update the cached object with latest data
                cached.setCurrentLocation(ambulance.getCurrentLocation());
                cached.setAvailability(ambulance.getAvailability());
                // Other fields as needed
            });
            
            // Rebuild queue with current available ambulances in cache
            availableQueue.clear();
            availableQueue.addAll(available.stream()
                .map(a -> ambulanceCache.get(a.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        }
        
        return new ArrayList<>(available);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public Optional<Ambulance> getNextAvailableAmbulance() {
        logger.debug("Starting getNextAvailableAmbulance. Queue size: {}", availableQueue.size());

        // First try to get from queue
        Ambulance ambulance = availableQueue.poll();
        while (ambulance != null) {
            try {
                logger.debug("Processing ambulance ID: {}", ambulance.getId());

                // Re-check against database for the most current status with PESSIMISTIC_WRITE lock
                Optional<Ambulance> currentOpt = ambulanceRepository.findByIdWithPessimisticWriteLock(ambulance.getId());

                if (currentOpt.isPresent()) {
                    Ambulance current = currentOpt.get();

                    if (current.getAvailability() == AvailabilityStatus.AVAILABLE) {
                        // Update status to DISPATCHED
                        current.setAvailability(AvailabilityStatus.DISPATCHED);
                        Ambulance updated = ambulanceRepository.save(current);

                        // Update cache
                        updateCacheAndQueue(updated);
                        logger.info("Successfully dispatched ambulance: {}", current.getId());
                        return Optional.of(updated);
                    } else {
                        logger.debug("Ambulance {} no longer available (status: {})",
                                current.getId(), current.getAvailability());
                    }
                }

                // Get next ambulance
                ambulance = availableQueue.poll();

            } catch (ObjectOptimisticLockingFailureException e) {
                logger.warn("Optimistic lock failure while processing ambulance {}, trying next",
                    ambulance != null ? ambulance.getId() : "null");
                // Try next ambulance
                ambulance = availableQueue.poll();
            } catch (Exception e) {
                logger.error("Error processing ambulance {}: {}",
                        ambulance != null ? ambulance.getId() : "null", e.getMessage(), e);
                // Move to next ambulance
                ambulance = availableQueue.poll();
            }
        }

        // If we get here, no available ambulances in queue, try database directly
        try {
            // Try to find an available ambulance with PESSIMISTIC_WRITE lock
            Optional<Ambulance> availableAmbulance = ambulanceRepository
                    .findFirstByAvailabilityWithPessimisticWriteLock(AvailabilityStatus.AVAILABLE);

            if (availableAmbulance.isPresent()) {
                Ambulance dbAmbulance = availableAmbulance.get();
                dbAmbulance.setAvailability(AvailabilityStatus.DISPATCHED);
                Ambulance updated = ambulanceRepository.save(dbAmbulance);
                updateCacheAndQueue(updated);
                logger.info("Found and dispatched available ambulance from database: {}", updated.getId());
                return Optional.of(updated);
            }
        } catch (Exception e) {
            logger.error("Error finding available ambulance from database: {}", e.getMessage(), e);
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

    // Add this method to the interface if not already present
    public interface AmbulanceServiceInterface {
        List<Ambulance> getAllAmbulances();
        Optional<Ambulance> getAmbulanceById(Long id);
        Ambulance saveAmbulance(Ambulance ambulance);
        void updateAmbulanceStatus(Long ambulanceId, AvailabilityStatus newStatus);
        List<Ambulance> getAvailableAmbulances();
        Optional<Ambulance> getNextAvailableAmbulance();
        long countAllAmbulances();
        long countAmbulancesByStatus(AvailabilityStatus status);
    }
}