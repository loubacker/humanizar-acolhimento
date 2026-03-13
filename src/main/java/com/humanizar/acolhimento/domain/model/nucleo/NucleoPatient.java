package com.humanizar.acolhimento.domain.model.nucleo;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class NucleoPatient {

    private UUID id;
    private UUID patientId;
    private UUID nucleoId;
    private List<NucleoPatientResponsavel> nucleoPatientResponsavel;

    // Construtores
    public NucleoPatient() {
    }

    public NucleoPatient(UUID id, UUID patientId, UUID nucleoId,
            List<NucleoPatientResponsavel> nucleoPatientResponsavel) {
        this.id = id;
        this.patientId = patientId;
        this.nucleoId = nucleoId;
        this.nucleoPatientResponsavel = nucleoPatientResponsavel;
    }

    public NucleoPatient(UUID patientId, UUID nucleoId, List<NucleoPatientResponsavel> nucleoPatientResponsavel) {
        this(null, patientId, nucleoId, nucleoPatientResponsavel);
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

    public UUID getNucleoId() {
        return nucleoId;
    }

    public void setNucleoId(UUID nucleoId) {
        this.nucleoId = nucleoId;
    }

    public List<NucleoPatientResponsavel> getNucleoPatientResponsavel() {
        return nucleoPatientResponsavel;
    }

    public void setNucleoPatientResponsavel(List<NucleoPatientResponsavel> nucleoPatientResponsavel) {
        this.nucleoPatientResponsavel = nucleoPatientResponsavel;
    }

    // Builder Estático
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID patientId;
        private UUID nucleoId;
        private List<NucleoPatientResponsavel> nucleoPatientResponsavel;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder patientId(UUID patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder nucleoId(UUID nucleoId) {
            this.nucleoId = nucleoId;
            return this;
        }

        public Builder nucleoPatientResponsavel(List<NucleoPatientResponsavel> nucleoPatientResponsavel) {
            this.nucleoPatientResponsavel = nucleoPatientResponsavel;
            return this;
        }

        public NucleoPatient build() {
            return new NucleoPatient(id, patientId, nucleoId, nucleoPatientResponsavel);
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
        NucleoPatient that = (NucleoPatient) o;
        return Objects.equals(id, that.id)
                && Objects.equals(patientId, that.patientId)
                && Objects.equals(nucleoId, that.nucleoId)
                && Objects.equals(nucleoPatientResponsavel, that.nucleoPatientResponsavel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, patientId, nucleoId, nucleoPatientResponsavel);
    }

    @Override
    public String toString() {
        return "NucleoPatient{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", nucleoId=" + nucleoId +
                ", nucleoPatientResponsavel=" + nucleoPatientResponsavel +
                '}';
    }
}
