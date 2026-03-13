package com.humanizar.acolhimento.infrastructure.persistence.entity.peding;

import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "pending_target_status", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_target", columnNames = { "event_id", "target_service" })
}, indexes = {
        @Index(name = "idx_target_status", columnList = "target_service,status"),
        @Index(name = "idx_event_status", columnList = "event_id,status")
})
public class PendingTargetStatusEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "target_service", nullable = false)
    private String targetService;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    public PendingTargetStatusEntity() {
    }

    public PendingTargetStatusEntity(UUID id, UUID eventId, String targetService, Status status) {
        this.id = id;
        this.eventId = eventId;
        this.targetService = targetService;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PendingTargetStatusEntity that = (PendingTargetStatusEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(eventId, that.eventId)
                && Objects.equals(targetService, that.targetService)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventId, targetService, status);
    }

    @Override
    public String toString() {
        return "PendingTargetStatusEntity{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", targetService='" + targetService + '\'' +
                ", status=" + status +
                '}';
    }
}
