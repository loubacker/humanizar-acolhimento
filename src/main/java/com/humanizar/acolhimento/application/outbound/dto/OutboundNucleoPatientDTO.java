package com.humanizar.acolhimento.application.outbound.dto;

import java.util.List;
import java.util.UUID;

public record OutboundNucleoPatientDTO(
                UUID nucleoPatientId,
                UUID patientId,
                UUID nucleoId,
                List<OutboundNucleoResponsavelDTO> nucleoPatientResponsavel) {
}
