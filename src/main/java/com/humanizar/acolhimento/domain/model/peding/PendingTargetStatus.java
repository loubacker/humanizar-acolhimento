package com.humanizar.acolhimento.domain.model.peding;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.Status;

public class PendingTargetStatus {

    private UUID id;
    private UUID eventId;
    private String targetService;
    private Status status;
    private Integer recallAttempts;
    private LocalDateTime lastRecallAt;
    private LocalDateTime nextRecallAt;

    public PendingTargetStatus() {
    }

    public PendingTargetStatus(UUID id, UUID eventId, String targetService, Status status) {
        this(id, eventId, targetService, status, null, null, null);
    }

    public PendingTargetStatus(UUID eventId, String targetService, Status status) {
        this(null, eventId, targetService, status, null, null, null);
    }

    public PendingTargetStatus(UUID id, UUID eventId, String targetService, Status status,
            Integer recallAttempts, LocalDateTime lastRecallAt, LocalDateTime nextRecallAt) {
        this.id = id;
        this.eventId = eventId;
        this.targetService = targetService;
        this.status = status;
        this.recallAttempts = recallAttempts;
        this.lastRecallAt = lastRecallAt;
        this.nextRecallAt = nextRecallAt;
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

    public Integer getRecallAttempts() {
        return recallAttempts;
    }

    public void setRecallAttempts(Integer recallAttempts) {
        this.recallAttempts = recallAttempts;
    }

    public LocalDateTime getLastRecallAt() {
        return lastRecallAt;
    }

    public void setLastRecallAt(LocalDateTime lastRecallAt) {
        this.lastRecallAt = lastRecallAt;
    }

    public LocalDateTime getNextRecallAt() {
        return nextRecallAt;
    }

    public void setNextRecallAt(LocalDateTime nextRecallAt) {
        this.nextRecallAt = nextRecallAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID eventId;
        private String targetService;
        private Status status;
        private Integer recallAttempts;
        private LocalDateTime lastRecallAt;
        private LocalDateTime nextRecallAt;

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

        public Builder recallAttempts(Integer recallAttempts) {
            this.recallAttempts = recallAttempts;
            return this;
        }

        public Builder lastRecallAt(LocalDateTime lastRecallAt) {
            this.lastRecallAt = lastRecallAt;
            return this;
        }

        public Builder nextRecallAt(LocalDateTime nextRecallAt) {
            this.nextRecallAt = nextRecallAt;
            return this;
        }

        public PendingTargetStatus build() {
            return new PendingTargetStatus(id, eventId, targetService, status,
                    recallAttempts, lastRecallAt, nextRecallAt);
        }
    }

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
                ", recallAttempts=" + recallAttempts +
                ", lastRecallAt=" + lastRecallAt +
                ", nextRecallAt=" + nextRecallAt +
                '}';
    }
}
