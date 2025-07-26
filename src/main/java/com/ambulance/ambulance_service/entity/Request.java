package com.ambulance.ambulance_service.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requests")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "medical_notes")
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
        requestTime = LocalDateTime.now();
        status = RequestStatus.PENDING;
    }

    // Constructors
    public Request() {}

    public Request(String userName, String userContact, String location, String emergencyDescription) {
        this.userName = userName;
        this.userContact = userContact;
        this.location = location;
        this.emergencyDescription = emergencyDescription;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        // Only set the name from user, keep the contact info as provided
        if (user != null) {
            this.userName = user.getUsername();
            // Don't override userContact with email
        }
    }

    public String getUserName() {
        return user != null ? user.getUsername() : userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserContact() {
        return userContact; // Return the direct contact info, don't override with email
    }

    public void setUserContact(String userContact) {
        this.userContact = userContact;
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEmergencyDescription() { return emergencyDescription; }
    public void setEmergencyDescription(String emergencyDescription) { this.emergencyDescription = emergencyDescription; }

    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }

    public LocalDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }

    public LocalDateTime getDispatchTime() { return dispatchTime; }
    public void setDispatchTime(LocalDateTime dispatchTime) { this.dispatchTime = dispatchTime; }

    public Ambulance getAmbulance() { return ambulance; }
    public void setAmbulance(Ambulance ambulance) { this.ambulance = ambulance; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    /**
     * Updates the status of the request and records the change in history
     * @param newStatus The new status to set
     * @param changedBy The username of the user making the change
     * @param notes Optional notes about the status change
     */
    public void updateStatus(RequestStatus newStatus, String changedBy, String notes) {
        RequestStatus oldStatus = this.status;
        this.status = newStatus;
        
        // Record the status change in history
        RequestStatusHistory history = new RequestStatusHistory(
            this, 
            oldStatus, 
            newStatus, 
            notes, 
            changedBy
        );
        this.statusHistory.add(history);
        
        // Update dispatch time if being dispatched
        if (newStatus == RequestStatus.DISPATCHED && this.dispatchTime == null) {
            this.dispatchTime = LocalDateTime.now();
        }
    }
    
    public List<RequestStatusHistory> getStatusHistory() {
        return statusHistory;
    }
    
    public void setStatusHistory(List<RequestStatusHistory> statusHistory) {
        this.statusHistory = statusHistory;
    }
}