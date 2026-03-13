package com.humanizar.acolhimento.application.outbound;

import java.time.LocalDateTime;
import java.util.UUID;

public record CallbackDTO(
        String upStream,
        UUID eventId,
        UUID correlationId,
        String producerService,
        String exchangeName,
        String routingKey,
        String aggregateType,
        UUID aggregateId,
        int eventVersion,
        LocalDateTime occurredAt,
        UUID actorId,
        String userAgent,
        String originIp,
        String status,
        String reasonCode,
        String errorMessage,
        LocalDateTime processedAt,
        LocalDateTime rejectedAt) {
}
