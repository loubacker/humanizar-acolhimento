package com.humanizar.acolhimento.application.outbound.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OutboundEnvelopeDTO<T>(
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
                T payload) {
}
