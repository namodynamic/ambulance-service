package com.ambulance.ambulance_service.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requests")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Request extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User user;

    @Column(name = "user_name")
    private String userName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    @Column(name = "user_contact")
    private String userContact;

    @NotNull
    private String location;

    @Column(name = "emergency_description")
    private String emergencyDescription;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    @Column(name = "dispatch_time")
    private LocalDateTime dispatchTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Ambulance ambulance;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<RequestStatusHistory> statusHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (requestTime == null) {
            requestTime = LocalDateTime.now();
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }

    // Constructors
    public Request() {}

    public Request(String userName, String userContact, String location, String emergencyDescription) {
        this.userName = userName;
        this.userContact = userContact;
        this.location = location;
        this.emergencyDescription = emergencyDescription;
    }

    // Helper methods
    public String getUserName() {
        return user != null ? user.getUsername() : userName;
    }

    public void setUser(User user) {
        this.user = user;
        // Only set the name from user, keep the contact info as provided
        if (user != null) {
            this.userName = user.getUsername();
            // Don't override userContact with email
        }
    }

    @Override
    public String toString() {
        return "Request{" +
                "id=" + getId() +
                ", userName='" + userName + '\'' +
                ", userContact='" + userContact + '\'' +
                ", location='" + location + '\'' +
                ", emergencyDescription='" + emergencyDescription + '\'' +
                ", requestTime=" + requestTime +
                ", dispatchTime=" + dispatchTime +
                ", status=" + status +
                ", ambulance=" + (ambulance != null ? ambulance.getId() : "null") +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                ", deleted=" + isDeleted() +
                ", deletedAt=" + getDeletedAt() +
                '}';
    }
}