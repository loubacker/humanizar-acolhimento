package com.humanizar.acolhimento.infrastructure.persistence.entity.nucleo;

import java.util.Objects;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.ResponsavelRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "nucleo_patient_responsavel", uniqueConstraints = {
        @UniqueConstraint(name = "uk_npr_nucleo_responsavel", columnNames = { "nucleo_patient_id", "responsavel_id" })
}, indexes = {
        @Index(name = "idx_npr_nucleo_patient_id", columnList = "nucleo_patient_id")
})
public class NucleoPatientResponsavelEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nucleo_patient_id", nullable = false)
    private UUID nucleoPatientId;

    @Column(name = "responsavel_id", nullable = false)
    private UUID responsavelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ResponsavelRole role;

    public NucleoPatientResponsavelEntity() {
    }

    public NucleoPatientResponsavelEntity(UUID id, UUID nucleoPatientId, UUID responsavelId, ResponsavelRole role) {
        this.id = id;
        this.nucleoPatientId = nucleoPatientId;
        this.responsavelId = responsavelId;
        this.role = role;
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

    public UUID getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(UUID responsavelId) {
        this.responsavelId = responsavelId;
    }

    public ResponsavelRole getRole() {
        return role;
    }

    public void setRole(ResponsavelRole role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NucleoPatientResponsavelEntity that = (NucleoPatientResponsavelEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(nucleoPatientId, that.nucleoPatientId)
                && Objects.equals(responsavelId, that.responsavelId)
                && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nucleoPatientId, responsavelId, role);
    }

    @Override
    public String toString() {
        return "NucleoPatientResponsavelEntity{" +
                "id=" + id +
                ", nucleoPatientId=" + nucleoPatientId +
                ", responsavelId=" + responsavelId +
                ", role=" + role +
                '}';
    }
}
