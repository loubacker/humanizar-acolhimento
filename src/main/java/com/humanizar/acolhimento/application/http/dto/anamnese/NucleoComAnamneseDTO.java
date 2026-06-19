package com.humanizar.acolhimento.application.http.dto.anamnese;

import java.util.List;
import java.util.UUID;

import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoResponsavelDTO;

public record NucleoComAnamneseDTO(
        UUID nucleoId,
        List<NucleoResponsavelDTO> responsaveis,
        AnamneseDTO anamnese,
        String anamneseStatus) {
}
