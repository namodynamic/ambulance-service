package com.ambulance.ambulance_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Getter
@Setter
public class Patient extends BaseEntity {
    
    @NotNull
    private String name;

    @NotNull
    private String contact;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    // Constructor for JPA
    public Patient() {
    }

    // Convenience constructor
    public Patient(String name, String contact, String medicalNotes) {
        this.name = name;
        this.contact = contact;
        this.medicalNotes = medicalNotes;
    }

    // Override toString() for better logging
    @Override
    public String toString() {
        return "Patient{" +
                "id=" + super.getId() +
                ", name='" + name + '\'' +
                ", contact='" + contact + '\'' +
                ", medicalNotes='" + medicalNotes + '\'' +
                ", createdAt=" + super.getCreatedAt() +
                ", updatedAt=" + super.getUpdatedAt() +
                ", deleted=" + isDeleted() +
                ", deletedAt=" + getDeletedAt() +
                '}';
    }
}