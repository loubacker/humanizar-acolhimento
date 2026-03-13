package com.humanizar.acolhimento.infrastructure.persistence.repository.nucleo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.infrastructure.persistence.entity.nucleo.NucleoPatientResponsavelEntity;

@Repository
public interface NucleoPatientResponsavelRepository extends JpaRepository<NucleoPatientResponsavelEntity, UUID> {

    List<NucleoPatientResponsavelEntity> findByNucleoPatientId(UUID nucleoPatientId);

    void deleteByNucleoPatientId(UUID nucleoPatientId);

    void deleteByIdIn(List<UUID> ids);

    @Modifying
    @Query("DELETE FROM NucleoPatientResponsavelEntity r WHERE r.nucleoPatientId IN :nucleoPatientIds")
    int deleteByNucleoPatientIds(@Param("nucleoPatientIds") List<UUID> nucleoPatientIds);

    @Modifying
    @Query("DELETE FROM NucleoPatientResponsavelEntity r WHERE r.nucleoPatientId IN " +
            "(SELECT n.id FROM NucleoPatientEntity n WHERE n.patientId = :patientId)")
    int deleteByPatientId(@Param("patientId") UUID patientId);
}
