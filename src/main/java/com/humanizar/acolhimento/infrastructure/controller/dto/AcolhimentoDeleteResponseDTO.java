package com.humanizar.acolhimento.infrastructure.controller.dto;

import java.util.UUID;

public record AcolhimentoDeleteResponseDTO(
        String status,
        String operation,
        UUID patientId) {
}
