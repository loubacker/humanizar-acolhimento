package com.humanizar.acolhimento.application.inbound.dto.nucleo;

import java.util.List;
import java.util.UUID;

public record NucleoPatientDTO(
                UUID nucleoId,
                List<NucleoResponsavelDTO> nucleoPatientResponsavel) {
}
