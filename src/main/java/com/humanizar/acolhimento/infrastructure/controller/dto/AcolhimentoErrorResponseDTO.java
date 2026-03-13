package com.humanizar.acolhimento.infrastructure.controller.dto;

import java.time.OffsetDateTime;

public record AcolhimentoErrorResponseDTO(
                int status,
                String reasonCode,
                String message,
                String correlationId,
                String path,
                OffsetDateTime timestamp) {
}
