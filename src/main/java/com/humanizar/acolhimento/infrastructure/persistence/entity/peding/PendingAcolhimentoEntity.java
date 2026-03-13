package com.humanizar.acolhimento.infrastructure.persistence.entity.peding;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.OperationType;
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
@Table(name = "pending_acolhimento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pending_acolhimento_event_id", columnNames = "event_id"),
        @UniqueConstraint(name = "uk_pending_acolhimento_correlation_id", columnNames = "correlation_id")
}, indexes = {
        @Index(name = "idx_pending_correlation", columnList = "correlation_id"),
        @Index(name = "idx_pending_patient_created", columnList = "patient_id,created_at"),
        @Index(name = "idx_pending_status_created", columnList = "status,created_at")
})
public class PendingAcolhimentoEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "payload_snapshot", nullable = false, columnDefinition = "TEXT")
    private String payloadSnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PendingAcolhimentoEntity() {
    }

    public PendingAcolhimentoEntity(UUID eventId, UUID correlationId, UUID patientId,
            OperationType operationType, Status status, String payloadSnapshot, LocalDateTime createdAt) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PendingAcolhimentoEntity that = (PendingAcolhimentoEntity) o;
        return Objects.equals(eventId, that.eventId)
                && Objects.equals(correlationId, that.correlationId)
                && Objects.equals(patientId, that.patientId)
                && operationType == that.operationType
                && status == that.status
                && Objects.equals(payloadSnapshot, that.payloadSnapshot)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, correlationId, patientId, operationType, status, payloadSnapshot, createdAt);
    }

    @Override
    public String toString() {
        return "PendingAcolhimentoEntity{" +
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
