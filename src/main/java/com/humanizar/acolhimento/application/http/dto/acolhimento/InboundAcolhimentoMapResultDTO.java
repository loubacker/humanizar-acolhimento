package com.humanizar.acolhimento.application.http.dto.acolhimento;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;

public record InboundAcolhimentoMapResultDTO(
        UUID patientId,
        List<NucleoPatient> nucleoPatients,
        List<NucleoPatientResponsavel> nucleoPatientResponsaveis,
        Map<UUID, UUID> nucleoPatientIdsByNucleoId) {

    public InboundAcolhimentoMapResultDTO {
        nucleoPatients = List.copyOf(nucleoPatients);
        nucleoPatientResponsaveis = List.copyOf(nucleoPatientResponsaveis);
        nucleoPatientIdsByNucleoId = Map.copyOf(nucleoPatientIdsByNucleoId);
    }
}
