package com.algotrade.authservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status = EntityStatus.ACTV;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = EntityStatus.ACTV;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Soft delete helper
    public void setStatusInactive() {
        this.status = EntityStatus.ICTV;
    }

    // Restore helper
    public void restoreStatus() {
        this.status = EntityStatus.ACTV;
    }

    public boolean isStatusActive() {
        return this.status == EntityStatus.ACTV;
    }
    
    public void softDeleteStatus() {
        this.status = EntityStatus.DELE;
    }
    
    public boolean isStatusDeleted() {
        return this.status == EntityStatus.DELE;
    }
}