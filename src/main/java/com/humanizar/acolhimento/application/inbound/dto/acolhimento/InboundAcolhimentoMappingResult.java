package com.humanizar.acolhimento.application.inbound.dto.acolhimento;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;

public record InboundAcolhimentoMappingResult(
        Acolhimento acolhimento,
        List<NucleoPatient> nucleoPatients,
        List<NucleoPatientResponsavel> nucleoPatientResponsaveis,
        Map<UUID, UUID> nucleoPatientIdsByNucleoId) {

    public InboundAcolhimentoMappingResult {
        nucleoPatients = List.copyOf(nucleoPatients);
        nucleoPatientResponsaveis = List.copyOf(nucleoPatientResponsaveis);
        nucleoPatientIdsByNucleoId = Map.copyOf(nucleoPatientIdsByNucleoId);
    }
}
