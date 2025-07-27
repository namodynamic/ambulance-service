package com.ambulance.ambulance_service.repository;

import com.ambulance.ambulance_service.entity.Ambulance;
import com.ambulance.ambulance_service.entity.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    @Query("SELECT a FROM Ambulance a WHERE a.availability = :status")
    List<Ambulance> findByAvailability(@Param("status") AvailabilityStatus status);

    @Query("SELECT a FROM Ambulance a WHERE a.availability = 'AVAILABLE' ORDER BY a.id LIMIT 1")
    Optional<Ambulance> findFirstAvailableAmbulance();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Ambulance a WHERE a.id = :id")
    Optional<Ambulance> findByIdWithPessimisticWriteLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Ambulance a WHERE a.availability = :status ORDER BY a.id")
    List<Ambulance> findByAvailabilityWithPessimisticWriteLock(@Param("status") AvailabilityStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Ambulance a WHERE a.availability = :status ORDER BY a.id LIMIT 1")
    Optional<Ambulance> findFirstByAvailabilityWithPessimisticWriteLock(@Param("status") AvailabilityStatus status);

    Optional<Ambulance> findFirstByAvailability(AvailabilityStatus availabilityStatus);

    Optional<Ambulance> findByLicensePlate(String licensePlate);

    long countByAvailability(AvailabilityStatus status);

    @Modifying
    @Query("UPDATE Ambulance a SET a.availability = :newStatus, a.version = a.version + 1 " +
            "WHERE a.id = :id AND a.version = :version")
    int updateAmbulanceStatusWithLock(
            @Param("id") Long id,
            @Param("newStatus") AvailabilityStatus newStatus,
            @Param("version") Long version
    );
}