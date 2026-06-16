package com.humanizar.acolhimento.application.messaging.outbound.dto;

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

        public OutboundEnvelopeDTO<T> withRoutingKey(String newRoutingKey) {
                return new OutboundEnvelopeDTO<>(
                                eventId,
                                correlationId,
                                producerService,
                                exchangeName,
                                newRoutingKey,
                                aggregateType,
                                aggregateId,
                                eventVersion,
                                occurredAt,
                                actorId,
                                userAgent,
                                originIp,
                                payload);
        }
}
