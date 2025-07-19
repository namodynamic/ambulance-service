package com.ambulance.ambulance_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_name")
    private String userName;

    @NotNull
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    @Column(name = "user_contact")
    private String userContact;

    @NotNull
    private String location;

    @Column(name = "emergency_description")
    private String emergencyDescription;

    @Column(name = "request_time")
    private LocalDateTime requestTime;

    @Column(name = "dispatch_time")
    private LocalDateTime dispatchTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

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

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserContact() { return userContact; }
    public void setUserContact(String userContact) { this.userContact = userContact; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEmergencyDescription() { return emergencyDescription; }
    public void setEmergencyDescription(String emergencyDescription) { this.emergencyDescription = emergencyDescription; }

    public LocalDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }

    public LocalDateTime getDispatchTime() { return dispatchTime; }
    public void setDispatchTime(LocalDateTime dispatchTime) { this.dispatchTime = dispatchTime; }

    public Ambulance getAmbulance() { return ambulance; }
    public void setAmbulance(Ambulance ambulance) { this.ambulance = ambulance; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
}