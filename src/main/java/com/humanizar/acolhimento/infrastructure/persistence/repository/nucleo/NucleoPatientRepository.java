package com.humanizar.acolhimento.infrastructure.persistence.repository.nucleo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.infrastructure.persistence.entity.nucleo.NucleoPatientEntity;

@Repository
public interface NucleoPatientRepository extends JpaRepository<NucleoPatientEntity, UUID> {

    List<NucleoPatientEntity> findByPatientId(UUID patientId);

    @Modifying
    @Query("update NucleoPatientEntity n set n.anamneseStatus = :status where n.id = :id")
    void updateAnamneseStatus(@Param("id") UUID id, @Param("status") AnamneseStatus status);

    Optional<NucleoPatientEntity> findByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);

    boolean existsByPatientId(UUID patientId);

    void deleteByPatientId(UUID patientId);

    void deleteByIdIn(List<UUID> ids);

    void deleteByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);
}
