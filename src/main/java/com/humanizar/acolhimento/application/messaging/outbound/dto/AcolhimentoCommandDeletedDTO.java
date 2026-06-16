package com.humanizar.acolhimento.application.messaging.outbound.dto;

import java.util.UUID;

public record AcolhimentoCommandDeletedDTO(
        UUID patientId) {
}
