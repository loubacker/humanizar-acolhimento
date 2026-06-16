package com.humanizar.acolhimento.infrastructure.controller.dto;

import java.time.LocalDateTime;

public record SuccessResponseDTO(
        String code,
        boolean success,
        String correlationId,
        String message,
        Object data,
        LocalDateTime timestamp) {

    public SuccessResponseDTO(String code, String correlationId, String message, Object data) {
        this(code, true, correlationId, message, data, LocalDateTime.now());
    }
}
