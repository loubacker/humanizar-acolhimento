package com.humanizar.acolhimento.domain.port.nucleo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;

public interface NucleoPatientPort {

    List<NucleoPatient> saveAll(List<NucleoPatient> nucleos);

    void updateAnamneseStatus(UUID nucleoPatientId, AnamneseStatus status);

    List<NucleoPatient> findByPatientId(UUID patientId);

    Optional<NucleoPatient> findByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);

    boolean existsByPatientId(UUID patientId);

    void deleteByPatientId(UUID patientId);

    void deleteByIds(List<UUID> ids);

    void deleteByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);
}
