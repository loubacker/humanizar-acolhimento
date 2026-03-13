package com.humanizar.acolhimento.domain.port.nucleo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;

public interface NucleoPatientPort {

    List<NucleoPatient> saveAll(List<NucleoPatient> nucleos);

    List<NucleoPatient> findByPatientId(UUID patientId);

    Optional<NucleoPatient> findByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);

    void deleteByPatientId(UUID patientId);

    void deleteByIds(List<UUID> ids);

    void deleteByPatientIdAndNucleoId(UUID patientId, UUID nucleoId);
}
