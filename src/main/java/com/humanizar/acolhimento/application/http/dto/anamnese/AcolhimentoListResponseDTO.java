package com.humanizar.acolhimento.application.http.dto.anamnese;

import java.util.List;
import java.util.UUID;

public record AcolhimentoListResponseDTO(
        UUID patientId,
        List<NucleoComAnamneseDTO> nucleos) {
}
