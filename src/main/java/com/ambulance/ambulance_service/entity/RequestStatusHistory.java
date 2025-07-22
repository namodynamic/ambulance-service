package com.ambulance.ambulance_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_status_history")
@Data
public class RequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnore
    private Request request;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private RequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private RequestStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_by")
    private String changedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Default constructor for JPA
    public RequestStatusHistory() {}

    // Convenience constructor
    public RequestStatusHistory(Request request, RequestStatus oldStatus,
                              RequestStatus newStatus, String notes, String changedBy) {
        this.request = request;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.notes = notes;
        this.changedBy = changedBy;
    }
}
