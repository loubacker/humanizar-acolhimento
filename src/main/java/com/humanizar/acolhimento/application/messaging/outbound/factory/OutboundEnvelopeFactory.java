package com.humanizar.acolhimento.application.messaging.outbound.factory;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.ProducerCatalog;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundEnvelopeDTO;

@Component
public class OutboundEnvelopeFactory {

    public <T> OutboundEnvelopeDTO<T> command(
            HttpEnvelopeDTO<?> inboundEnvelope,
            UUID eventId,
            String routingKey,
            UUID aggregateId,
            T payload) {
        return new OutboundEnvelopeDTO<>(
                eventId,
                inboundEnvelope.correlationId(),
                ProducerCatalog.PRODUCER_SERVICE,
                ExchangeCatalog.ACOLHIMENTO_COMMAND,
                routingKey,
                ProducerCatalog.AGGREGATE_TYPE,
                aggregateId,
                ProducerCatalog.EVENT_VERSION,
                LocalDateTime.now(),
                inboundEnvelope.actorId(),
                inboundEnvelope.userAgent(),
                inboundEnvelope.originIp(),
                payload);
    }
}
