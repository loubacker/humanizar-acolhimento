package com.humanizar.acolhimento.infrastructure.persistence.entity.nucleo;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "nucleo_patient", uniqueConstraints = {
        @UniqueConstraint(name = "uk_patient_nucleo", columnNames = { "patient_id", "nucleo_id" })
}, indexes = {
        @Index(name = "idx_nucleo_patient", columnList = "patient_id")
})
public class NucleoPatientEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "nucleo_id", nullable = false)
    private UUID nucleoId;

    public NucleoPatientEntity() {
    }

    public NucleoPatientEntity(UUID id, UUID patientId, UUID nucleoId) {
        this.id = id;
        this.patientId = patientId;
        this.nucleoId = nucleoId;
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

    public UUID getNucleoId() {
        return nucleoId;
    }

    public void setNucleoId(UUID nucleoId) {
        this.nucleoId = nucleoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NucleoPatientEntity that = (NucleoPatientEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(patientId, that.patientId)
                && Objects.equals(nucleoId, that.nucleoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, patientId, nucleoId);
    }

    @Override
    public String toString() {
        return "NucleoPatientEntity{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", nucleoId=" + nucleoId +
                '}';
    }
}
