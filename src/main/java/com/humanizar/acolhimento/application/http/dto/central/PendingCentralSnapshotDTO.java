package com.humanizar.acolhimento.application.http.dto.central;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonRawValue;

public record PendingCentralSnapshotDTO(
        UUID eventId,
        UUID correlationId,
        UUID patientId,
        String operationType,
        String status,
        LocalDateTime createdAt,
        @JsonRawValue String payloadSnapshot,
        List<PendingTargetStatusDTO> targets) {
}
