package com.humanizar.acolhimento.infrastructure.controller.dto;

import java.util.UUID;

public record AcolhimentoCreateResponseDTO(
        String message,
        UUID patientId,
        UUID correlationId) {
}
