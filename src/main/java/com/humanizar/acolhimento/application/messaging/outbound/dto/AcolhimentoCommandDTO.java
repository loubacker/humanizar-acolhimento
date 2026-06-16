package com.humanizar.acolhimento.application.messaging.outbound.dto;

import java.util.List;
import java.util.UUID;

public record AcolhimentoCommandDTO(
        UUID patientId,
        List<OutboundNucleoPatientDTO> nucleoPatient) {
}
