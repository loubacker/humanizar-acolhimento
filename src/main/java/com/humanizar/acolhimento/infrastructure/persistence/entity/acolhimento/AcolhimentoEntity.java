package com.humanizar.acolhimento.infrastructure.persistence.entity.acolhimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "acolhimento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_acolhimento_patient_id", columnNames = "patient_id")
}, indexes = {
        @Index(name = "idx_acolhimento_updated_at", columnList = "updated_at")
})
public class AcolhimentoEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "data_anamnese")
    private LocalDate dataAnamnese;

    @Column(name = "hora_anamnese")
    private LocalTime horaAnamnese;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AcolhimentoEntity() {
    }

    public AcolhimentoEntity(UUID id, UUID patientId, LocalDate dataAnamnese, LocalTime horaAnamnese,
            String observacoes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.patientId = patientId;
        this.dataAnamnese = dataAnamnese;
        this.horaAnamnese = horaAnamnese;
        this.observacoes = observacoes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public LocalDate getDataAnamnese() {
        return dataAnamnese;
    }

    public void setDataAnamnese(LocalDate dataAnamnese) {
        this.dataAnamnese = dataAnamnese;
    }

    public LocalTime getHoraAnamnese() {
        return horaAnamnese;
    }

    public void setHoraAnamnese(LocalTime horaAnamnese) {
        this.horaAnamnese = horaAnamnese;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AcolhimentoEntity that = (AcolhimentoEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(patientId, that.patientId)
                && Objects.equals(dataAnamnese, that.dataAnamnese)
                && Objects.equals(horaAnamnese, that.horaAnamnese)
                && Objects.equals(observacoes, that.observacoes)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, patientId, dataAnamnese, horaAnamnese, observacoes, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "AcolhimentoEntity{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", dataAnamnese=" + dataAnamnese +
                ", horaAnamnese=" + horaAnamnese +
                ", observacoes='" + observacoes + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
