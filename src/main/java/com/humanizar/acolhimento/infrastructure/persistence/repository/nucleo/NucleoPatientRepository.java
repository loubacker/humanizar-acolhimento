package com.humanizar.acolhimento.infrastructure.persistence.repository.nucleo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.infrastructure.persistence.entity.nucleo.NucleoPatientEntity;

@Repository
public interface NucleoPatientRepository extends JpaRepository<NucleoPatientEntity, UUID> {

    List<NucleoPatientEntity> findByPatientId(UUID patientId);

    Optional<NucleoPatientEntity> findByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);

    void deleteByPatientId(UUID patientId);

    void deleteByIdIn(List<UUID> ids);

    void deleteByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);
}
