package com.humanizar.acolhimento.application.messaging.outbound.factory;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.ProducerCatalog;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;

@Component
public class CommandOutboxEventFactory {

    public OutboxEvent newCommand(
            UUID eventId,
            UUID correlationId,
            String routingKey,
            UUID aggregateId,
            String payload,
            UUID actorId,
            String userAgent,
            String originIp) {
        return OutboxEvent.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .producerService(ProducerCatalog.PRODUCER_SERVICE)
                .exchangeName(ExchangeCatalog.ACOLHIMENTO_COMMAND)
                .routingKey(routingKey)
                .aggregateType(ProducerCatalog.AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .eventVersion(ProducerCatalog.EVENT_VERSION)
                .payload(payload)
                .actorId(actorId)
                .userAgent(userAgent)
                .originIp(originIp)
                .status(OutboxStatus.NEW)
                .attemptCount(0)
                .maxAttempts(ProducerCatalog.DEFAULT_MAX_ATTEMPTS)
                .nextRetryAt(LocalDateTime.now())
                .build();
    }
}
