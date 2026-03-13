package com.humanizar.acolhimento.domain.model.acolhimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;

public class Acolhimento {

    private UUID id;
    private UUID patientId;
    private LocalDate dataAnamnese;
    private LocalTime horaAnamnese;
    private String observacoes;
    private List<NucleoPatient> nucleoPatient;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Construtores
    public Acolhimento() {
    }

    public Acolhimento(UUID id, UUID patientId, LocalDate dataAnamnese, LocalTime horaAnamnese, String observacoes,
            List<NucleoPatient> nucleoPatient, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.patientId = patientId;
        this.dataAnamnese = dataAnamnese;
        this.horaAnamnese = horaAnamnese;
        this.observacoes = observacoes;
        this.nucleoPatient = nucleoPatient;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Acolhimento(UUID patientId, LocalDate dataAnamnese, LocalTime horaAnamnese, String observacoes,
            List<NucleoPatient> nucleoPatient, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(null, patientId, dataAnamnese, horaAnamnese, observacoes, nucleoPatient, createdAt, updatedAt);
    }

    // Getters e Setters
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

    public List<NucleoPatient> getNucleoPatient() {
        return nucleoPatient;
    }

    public void setNucleoPatient(List<NucleoPatient> nucleoPatient) {
        this.nucleoPatient = nucleoPatient;
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

    // Builder Estático
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID patientId;
        private LocalDate dataAnamnese;
        private LocalTime horaAnamnese;
        private String observacoes;
        private List<NucleoPatient> nucleoPatient;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
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

        public Builder nucleoPatient(List<NucleoPatient> nucleoPatient) {
            this.nucleoPatient = nucleoPatient;
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
            return new Acolhimento(id, patientId, dataAnamnese, horaAnamnese, observacoes, nucleoPatient, createdAt,
                    updatedAt);
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
        Acolhimento that = (Acolhimento) o;
        return Objects.equals(id, that.id)
                && Objects.equals(patientId, that.patientId)
                && Objects.equals(dataAnamnese, that.dataAnamnese)
                && Objects.equals(horaAnamnese, that.horaAnamnese)
                && Objects.equals(observacoes, that.observacoes)
                && Objects.equals(nucleoPatient, that.nucleoPatient)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, patientId, dataAnamnese, horaAnamnese, observacoes, nucleoPatient, createdAt,
                updatedAt);
    }

    @Override
    public String toString() {
        return "Acolhimento{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", dataAnamnese=" + dataAnamnese +
                ", horaAnamnese=" + horaAnamnese +
                ", observacoes='" + observacoes + '\'' +
                ", nucleoPatient=" + nucleoPatient +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
