package com.humanizar.acolhimento.infrastructure.persistence.repository.acolhimento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.infrastructure.persistence.entity.acolhimento.AcolhimentoEntity;

@Repository
public interface AcolhimentoRepository extends JpaRepository<AcolhimentoEntity, UUID> {

    Optional<AcolhimentoEntity> findByNucleoPatientId(UUID nucleoPatientId);

    boolean existsByNucleoPatientId(UUID nucleoPatientId);

    List<AcolhimentoEntity> findByNucleoPatientIdIn(List<UUID> nucleoPatientIds);

    void deleteByNucleoPatientId(UUID nucleoPatientId);

    @Modifying
    @Query("DELETE FROM AcolhimentoEntity a WHERE a.nucleoPatientId IN " +
            "(SELECT n.id FROM NucleoPatientEntity n WHERE n.patientId = :patientId)")
    int deleteByPatientId(@Param("patientId") UUID patientId);
}
