package com.humanizar.acolhimento.domain.model.peding;

import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.Status;

public class PendingTargetStatus {

    private UUID id;
    private UUID eventId;
    private String targetService;
    private Status status;

    // Construtores
    public PendingTargetStatus() {
    }

    public PendingTargetStatus(UUID id, UUID eventId, String targetService, Status status) {
        this.id = id;
        this.eventId = eventId;
        this.targetService = targetService;
        this.status = status;
    }

    public PendingTargetStatus(UUID eventId, String targetService, Status status) {
        this(null, eventId, targetService, status);
    }

    // Getters e Setters
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

    // Builder Estático
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID eventId;
        private String targetService;
        private Status status;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder targetService(String targetService) {
            this.targetService = targetService;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public PendingTargetStatus build() {
            return new PendingTargetStatus(id, eventId, targetService, status);
        }
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PendingTargetStatus that = (PendingTargetStatus) o;
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
        return "PendingTargetStatus{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", targetService='" + targetService + '\'' +
                ", status=" + status +
                '}';
    }
}
