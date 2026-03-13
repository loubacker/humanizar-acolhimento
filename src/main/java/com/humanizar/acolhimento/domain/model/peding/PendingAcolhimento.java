package com.humanizar.acolhimento.domain.model.peding;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;

public class PendingAcolhimento {

    private UUID eventId;
    private UUID correlationId;
    private UUID patientId;
    private OperationType operationType;
    private Status status;
    private String payloadSnapshot;
    private LocalDateTime createdAt;

    // Construtores
    public PendingAcolhimento() {
    }

    public PendingAcolhimento(UUID eventId, UUID correlationId, UUID patientId,
            OperationType operationType, Status status, String payloadSnapshot,
            LocalDateTime createdAt) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.patientId = patientId;
        this.operationType = operationType;
        this.status = status;
        this.payloadSnapshot = payloadSnapshot;
        this.createdAt = createdAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPayloadSnapshot() {
        return payloadSnapshot;
    }

    public void setPayloadSnapshot(String payloadSnapshot) {
        this.payloadSnapshot = payloadSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Builder Estático
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID eventId;
        private UUID correlationId;
        private UUID patientId;
        private OperationType operationType;
        private Status status;
        private String payloadSnapshot;
        private LocalDateTime createdAt;

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder operationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder payloadSnapshot(String payloadSnapshot) {
            this.payloadSnapshot = payloadSnapshot;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PendingAcolhimento build() {
            return new PendingAcolhimento(eventId, correlationId, patientId, operationType, status,
                    payloadSnapshot, createdAt);
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
        PendingAcolhimento that = (PendingAcolhimento) o;
        return Objects.equals(correlationId, that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }

    @Override
    public String toString() {
        return "PendingAcolhimento{" +
                "eventId=" + eventId +
                ", correlationId=" + correlationId +
                ", patientId=" + patientId +
                ", operationType=" + operationType +
                ", status=" + status +
                ", payloadSnapshot='" + payloadSnapshot + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
