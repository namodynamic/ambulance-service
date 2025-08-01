package com.ambulance.ambulance_service.service;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import com.ambulance.ambulance_service.exception.AmbulanceNotFoundException;
import com.ambulance.ambulance_service.repository.AmbulanceRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Transactional
public class AmbulanceService implements AmbulanceServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(AmbulanceService.class);
    private static final long CACHE_REFRESH_INTERVAL = 300000; // 5 minutes in milliseconds

    private final AmbulanceRepository ambulanceRepository;
    private final Map<Long, Ambulance> ambulanceCache = new ConcurrentHashMap<>();
    private final Queue<Ambulance> availableQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    public AmbulanceService(AmbulanceRepository ambulanceRepository) {
        this.ambulanceRepository = ambulanceRepository;
    }

    @PostConstruct
    public void init() {
        try {
            loadAmbulances();
            logger.info("AmbulanceService initialized with {} ambulances ({} available)", 
                ambulanceCache.size(), availableQueue.size());
        } catch (Exception e) {
            logger.error("Failed to initialize AmbulanceService: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize AmbulanceService", e);
        }
    }

    @Scheduled(fixedRate = CACHE_REFRESH_INTERVAL)
    public void refreshCache() {
        try {
            logger.debug("Refreshing ambulance cache...");
            loadAmbulances();
            logger.info("Ambulance cache refreshed: {} ambulances ({} available)", 
                ambulanceCache.size(), availableQueue.size());
        } catch (Exception e) {
            logger.error("Error refreshing ambulance cache: {}", e.getMessage(), e);
        }
    }

    private synchronized void loadAmbulances() {
        try {
            logger.debug("Loading ambulances from database...");
            List<Ambulance> allAmbulances = ambulanceRepository.findByDeletedFalse();
            
            // Clear existing state
            availableQueue.clear();
            ambulanceCache.clear();

            if (allAmbulances.isEmpty()) {
                logger.warn("No ambulances found in the database");
                return;
            }

            // Process each ambulance
            for (Ambulance ambulance : allAmbulances) {
                if (ambulance == null) {
                    logger.warn("Skipping null ambulance in database");
                    continue;
                }

                try {
                    Ambulance cachedAmbulance = new Ambulance();
                    cachedAmbulance.setId(ambulance.getId());
                    cachedAmbulance.setCurrentLocation(ambulance.getCurrentLocation());
                    cachedAmbulance.setAvailability(ambulance.getAvailability() != null ? 
                        ambulance.getAvailability() : AvailabilityStatus.AVAILABLE);
                    cachedAmbulance.setLicensePlate(ambulance.getLicensePlate());
                    cachedAmbulance.setDriverName(ambulance.getDriverName());
                    cachedAmbulance.setDriverContact(ambulance.getDriverContact());
                    cachedAmbulance.setModel(ambulance.getModel());
                    cachedAmbulance.setYear(ambulance.getYear());
                    cachedAmbulance.setCapacity(ambulance.getCapacity());

                    ambulanceCache.put(cachedAmbulance.getId(), cachedAmbulance);

                    if (cachedAmbulance.getAvailability() == AvailabilityStatus.AVAILABLE) {
                        availableQueue.offer(cachedAmbulance);
                        logger.debug("Added available ambulance {} to queue", cachedAmbulance.getId());
                    }
                } catch (Exception e) {
                    logger.error("Error processing ambulance with ID {}: {}", 
                        ambulance.getId(), e.getMessage(), e);
                }
            }

            logger.info("Loaded {} ambulances ({} available) into cache", 
                ambulanceCache.size(), availableQueue.size());
                    
        } catch (DataAccessException e) {
            logger.error("Database error while loading ambulances: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to load ambulances from database", e);
        } catch (Exception e) {
            logger.error("Unexpected error while loading ambulances: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to load ambulances", e);
        }
    }

    @Override
    public List<Ambulance> getAllAmbulances() {
        if (ambulanceCache.isEmpty()) {
            logger.warn("Ambulance cache is empty, attempting to reload...");
            loadAmbulances();
            
            if (ambulanceCache.isEmpty()) {
                logger.error("No ambulances available in the system");
                return Collections.emptyList();
            }
        }
        return new ArrayList<>(ambulanceCache.values());
    }

    @Override
    public Optional<Ambulance> getAmbulanceById(Long id) {
        if (id == null) {
            logger.warn("Attempted to get ambulance with null ID");
            return Optional.empty();
        }
        
        Ambulance ambulance = ambulanceCache.get(id);
        if (ambulance == null) {
            logger.debug("Ambulance with ID {} not found in cache, checking database...", id);
            try {
                ambulance = ambulanceRepository.findById(id).orElse(null);
                if (ambulance != null) {
                    ambulanceCache.put(ambulance.getId(), ambulance);
                    logger.debug("Added ambulance with ID {} to cache", id);
                }
            } catch (Exception e) {
                logger.error("Error fetching ambulance with ID {} from database: {}", id, e.getMessage(), e);
            }
        }
        return Optional.ofNullable(ambulance);
    }

    @Override
    public Ambulance saveAmbulance(Ambulance ambulance) {
        if (ambulance == null) {
            throw new IllegalArgumentException("Ambulance cannot be null");
        }

        try {
            Ambulance savedAmbulance = ambulanceRepository.save(ambulance);
            updateCacheAndQueue(savedAmbulance);
            logger.info("Saved ambulance with ID: {}", savedAmbulance.getId());
            return savedAmbulance;
        } catch (DataAccessException e) {
            logger.error("Database error while saving ambulance: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to save ambulance to database", e);
        } catch (Exception e) {
            logger.error("Unexpected error while saving ambulance: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to save ambulance", e);
        }
    }

    @Override
    public void updateAmbulanceStatus(Long id, AvailabilityStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("Ambulance ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        try {
            Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new AmbulanceNotFoundException("Ambulance not found with id: " + id));
            
            ambulance.setAvailability(status);
            Ambulance updatedAmbulance = ambulanceRepository.save(ambulance);
            updateCacheAndQueue(updatedAmbulance);
            
            logger.info("Updated ambulance {} status to {}", id, status);
        } catch (AmbulanceNotFoundException e) {
            logger.warn("Attempted to update non-existent ambulance with ID: {}", id);
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            logger.error("Optimistic locking failure while updating ambulance {}: {}", id, e.getMessage());
            throw new IllegalStateException("Ambulance was modified by another transaction. Please try again.", e);
        } catch (DataAccessException e) {
            logger.error("Database error while updating ambulance status: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to update ambulance status in database", e);
        } catch (Exception e) {
            logger.error("Unexpected error while updating ambulance status: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to update ambulance status", e);
        }
    }

    @Override
    public List<Ambulance> getAvailableAmbulances() {
        if (availableQueue.isEmpty()) {
            logger.debug("Available queue is empty, checking for available ambulances...");
            List<Ambulance> available = ambulanceRepository.findByAvailability(AvailabilityStatus.AVAILABLE);
            available.forEach(ambulance -> {
                if (!availableQueue.contains(ambulance)) {
                    availableQueue.offer(ambulance);
                }
            });
            
            if (availableQueue.isEmpty()) {
                logger.warn("No available ambulances found in the system");
            }
        }
        return new ArrayList<>(availableQueue);
    }

    @Override
    public Optional<Ambulance> getNextAvailableAmbulance() {
        Ambulance ambulance = availableQueue.poll();
        if (ambulance != null) {
            // Update the status in the database and cache
            updateAmbulanceStatus(ambulance.getId(), AvailabilityStatus.DISPATCHED);
        }
        return Optional.ofNullable(ambulance);
    }

    private void updateCacheAndQueue(Ambulance ambulance) {
        if (ambulance == null || ambulance.getId() == null) {
            return;
        }

        // Update cache
        ambulanceCache.put(ambulance.getId(), ambulance);
        
        // Update available queue
        if (ambulance.getAvailability() == AvailabilityStatus.AVAILABLE) {
            // Remove if already in queue to avoid duplicates
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
    public long countAllAmbulances() {
        try {
            return ambulanceRepository.count();
        } catch (Exception e) {
            logger.error("Error counting all ambulances: {}", e.getMessage(), e);
            return ambulanceCache.size(); // Fallback to cache size
        }
    }

    @Override
    public long countAmbulancesByStatus(AvailabilityStatus status) {
        if (status == null) {
            return 0;
        }
        
        try {
            return ambulanceRepository.countByAvailability(status);
        } catch (Exception e) {
            logger.error("Error counting ambulances by status {}: {}", status, e.getMessage(), e);
            // Fallback to counting in cache
            return ambulanceCache.values().stream()
                .filter(a -> status.equals(a.getAvailability()))
                .count();
        }
    }

    @Override
    public boolean deleteAmbulance(Long id) {
        if (id == null) {
            return false;
        }

        try {
            Optional<Ambulance> ambulanceOpt = ambulanceRepository.findById(id);
            if (ambulanceOpt.isEmpty()) {
                return false;
            }
            
            // Soft delete
            Ambulance ambulance = ambulanceOpt.get();
            ambulance.setDeleted(true);
            ambulance.setDeletedAt(LocalDateTime.now());
            ambulanceRepository.save(ambulance);
            
            // Update cache
            ambulanceCache.remove(id);
            availableQueue.removeIf(a -> a.getId().equals(id));
            
            logger.info("Soft deleted ambulance with ID: {}", id);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting ambulance with ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<Ambulance> updateAmbulance(Long id, Ambulance ambulanceDetails) {
        if (id == null || ambulanceDetails == null) {
            return Optional.empty();
        }

        try {
            return ambulanceRepository.findById(id).map(ambulance -> {
                // Update fields
                if (ambulanceDetails.getCurrentLocation() != null) {
                    ambulance.setCurrentLocation(ambulanceDetails.getCurrentLocation());
                }
                if (ambulanceDetails.getAvailability() != null) {
                    ambulance.setAvailability(ambulanceDetails.getAvailability());
                }
                if (ambulanceDetails.getLicensePlate() != null) {
                    ambulance.setLicensePlate(ambulanceDetails.getLicensePlate());
                }
                if (ambulanceDetails.getDriverName() != null) {
                    ambulance.setDriverName(ambulanceDetails.getDriverName());
                }
                if (ambulanceDetails.getDriverContact() != null) {
                    ambulance.setDriverContact(ambulanceDetails.getDriverContact());
                }
                if (ambulanceDetails.getModel() != null) {
                    ambulance.setModel(ambulanceDetails.getModel());
                }
                if (ambulanceDetails.getYear() != null) {
                    ambulance.setYear(ambulanceDetails.getYear());
                }
                if (ambulanceDetails.getCapacity() != null) {
                    ambulance.setCapacity(ambulanceDetails.getCapacity());
                }

                Ambulance updatedAmbulance = ambulanceRepository.save(ambulance);
                updateCacheAndQueue(updatedAmbulance);
                logger.info("Updated ambulance with ID: {}", id);
                return updatedAmbulance;
            });
        } catch (Exception e) {
            logger.error("Error updating ambulance with ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Ambulance createAmbulance(Ambulance ambulance) {
        if (ambulance == null) {
            throw new IllegalArgumentException("Ambulance cannot be null");
        }

        try {
            // Set default availability if not provided
            if (ambulance.getAvailability() == null) {
                ambulance.setAvailability(AvailabilityStatus.AVAILABLE);
            }

            Ambulance savedAmbulance = ambulanceRepository.save(ambulance);
            updateCacheAndQueue(savedAmbulance);
            
            logger.info("Created new ambulance with ID: {}", savedAmbulance.getId());
            return savedAmbulance;
        } catch (Exception e) {
            logger.error("Error creating ambulance: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to create ambulance", e);
        }
    }

    @Override
    public Optional<Ambulance> findByLicensePlateIncludingDeleted(String licensePlate) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // First check the cache
            Optional<Ambulance> cachedAmbulance = ambulanceCache.values().stream()
                .filter(a -> licensePlate.equalsIgnoreCase(a.getLicensePlate()))
                .findFirst();

            if (cachedAmbulance.isPresent()) {
                return cachedAmbulance;
            }

            // If not in cache, check the database
            return ambulanceRepository.findByLicensePlate(licensePlate);
        } catch (Exception e) {
            logger.error("Error finding ambulance by license plate {}: {}", licensePlate, e.getMessage(), e);
            return Optional.empty();
        }
    }
}