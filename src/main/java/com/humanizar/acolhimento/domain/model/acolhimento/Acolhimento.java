package com.humanizar.acolhimento.domain.model.acolhimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class Acolhimento {

    private UUID id;
    private UUID nucleoPatientId;
    private LocalDate dataAnamnese;
    private LocalTime horaAnamnese;
    private String observacoes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Acolhimento() {
    }

    public Acolhimento(UUID id, UUID nucleoPatientId, LocalDate dataAnamnese, LocalTime horaAnamnese,
            String observacoes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.nucleoPatientId = nucleoPatientId;
        this.dataAnamnese = dataAnamnese;
        this.horaAnamnese = horaAnamnese;
        this.observacoes = observacoes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Acolhimento(UUID nucleoPatientId, LocalDate dataAnamnese, LocalTime horaAnamnese,
            String observacoes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(null, nucleoPatientId, dataAnamnese, horaAnamnese, observacoes, createdAt, updatedAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNucleoPatientId() {
        return nucleoPatientId;
    }

    public void setNucleoPatientId(UUID nucleoPatientId) {
        this.nucleoPatientId = nucleoPatientId;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID nucleoPatientId;
        private LocalDate dataAnamnese;
        private LocalTime horaAnamnese;
        private String observacoes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder nucleoPatientId(UUID nucleoPatientId) {
            this.nucleoPatientId = nucleoPatientId;
            return this;
        }

        public Builder dataAnamnese(LocalDate dataAnamnese) {
            this.dataAnamnese = dataAnamnese;
            return this;
        }

        public Builder horaAnamnese(LocalTime horaAnamnese) {
            this.horaAnamnese = horaAnamnese;
            return this;
        }

        public Builder observacoes(String observacoes) {
            this.observacoes = observacoes;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Acolhimento build() {
            return new Acolhimento(id, nucleoPatientId, dataAnamnese, horaAnamnese, observacoes, createdAt, updatedAt);
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
        Acolhimento that = (Acolhimento) o;
        return Objects.equals(id, that.id)
                && Objects.equals(nucleoPatientId, that.nucleoPatientId)
                && Objects.equals(dataAnamnese, that.dataAnamnese)
                && Objects.equals(horaAnamnese, that.horaAnamnese)
                && Objects.equals(observacoes, that.observacoes)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nucleoPatientId, dataAnamnese, horaAnamnese, observacoes, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Acolhimento{" +
                "id=" + id +
                ", nucleoPatientId=" + nucleoPatientId +
                ", dataAnamnese=" + dataAnamnese +
                ", horaAnamnese=" + horaAnamnese +
                ", observacoes='" + observacoes + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
