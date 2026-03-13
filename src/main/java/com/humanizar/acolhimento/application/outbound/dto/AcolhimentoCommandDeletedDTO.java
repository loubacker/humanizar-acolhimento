package com.humanizar.acolhimento.application.outbound.dto;

import java.util.UUID;

public record AcolhimentoCommandDeletedDTO(
        UUID patientId) {
}
