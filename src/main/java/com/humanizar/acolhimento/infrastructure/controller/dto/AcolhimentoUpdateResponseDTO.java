package com.humanizar.acolhimento.infrastructure.controller.dto;

import java.util.UUID;

public record AcolhimentoUpdateResponseDTO(
        String message,
        UUID patientId,
        UUID correlationId) {
}

