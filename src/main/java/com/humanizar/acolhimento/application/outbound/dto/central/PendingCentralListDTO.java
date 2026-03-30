package com.humanizar.acolhimento.application.outbound.dto.central;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PendingCentralListDTO(
        UUID eventId,
        UUID correlationId,
        UUID patientId,
        String operationType,
        String status,
        LocalDateTime createdAt,
        List<PendingTargetStatusDTO> targets) {
}
