package com.ambulance.ambulance_service.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ambulances")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Ambulance extends BaseEntity {

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull
    @Column(name = "current_location", nullable = false)
    private String currentLocation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'AVAILABLE'")
    private AvailabilityStatus availability = AvailabilityStatus.AVAILABLE;

    @Column(name = "license_plate", unique = true)
    private String licensePlate;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "driver_contact")
    private String driverContact;

    @Column(name = "model")
    private String model;

    @Column(name = "`year`") 
    private Integer year;

    @Column(name = "capacity")
    private Integer capacity;

    // Default constructor for JPA
    public Ambulance() {}

    // Convenience constructor
    public Ambulance(String currentLocation, AvailabilityStatus availability, String licensePlate) {
        this.currentLocation = currentLocation;
        this.availability = availability != null ? availability : AvailabilityStatus.AVAILABLE;
        this.licensePlate = licensePlate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ambulance ambulance = (Ambulance) o;
        return Objects.equals(getId(), ambulance.getId()) &&
               Objects.equals(version, ambulance.version) &&
               Objects.equals(currentLocation, ambulance.currentLocation) &&
               availability == ambulance.availability &&
               Objects.equals(licensePlate, ambulance.licensePlate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), version, currentLocation, availability, licensePlate);
    }

    @Override
    public String toString() {
        return "Ambulance{" +
                "id=" + getId() +
                ", version=" + version +
                ", currentLocation='" + currentLocation + '\'' +
                ", availability=" + availability +
                ", licensePlate='" + licensePlate + '\'' +
                ", driverName='" + driverName + '\'' +
                ", driverContact='" + driverContact + '\'' +
                ", model='" + model + '\'' +
                ", year=" + year +
                ", capacity=" + capacity +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                ", deleted=" + isDeleted() +
                ", deletedAt=" + getDeletedAt() +
                '}';
    }
}