package com.humanizar.acolhimento.domain.port.nucleo;

import java.util.List;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;

public interface NucleoPatientResponsavelPort {

    List<NucleoPatientResponsavel> saveAll(List<NucleoPatientResponsavel> responsaveis);

    List<NucleoPatientResponsavel> findByNucleoPatientId(UUID nucleoPatientId);

    void deleteByNucleoPatientId(UUID nucleoPatientId);

    void deleteByNucleoPatientIds(List<UUID> nucleoPatientIds);

    void deleteByIds(List<UUID> ids);

    void deleteByPatientId(UUID patientId);
}
