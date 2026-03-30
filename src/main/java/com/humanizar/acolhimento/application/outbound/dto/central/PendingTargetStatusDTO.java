package com.humanizar.acolhimento.application.outbound.dto.central;

public record PendingTargetStatusDTO(
        String targetService,
        String status) {
}
