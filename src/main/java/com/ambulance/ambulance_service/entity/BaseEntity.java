package com.ambulance.ambulance_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE ${entityName} SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Lifecycle callback method that is called before the entity is persisted.
     * Can be overridden by child classes to add custom initialization logic.
     */
    @PrePersist
    protected void onCreate() {
    }

    @PreRemove
    private void preRemove() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
