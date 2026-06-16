package com.humanizar.acolhimento.application.usecase.anamnese;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.ProducerCatalog;
import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@Service
public class CreateAnamneseAuditOutboxUseCase {

    private static final String AGGREGATE_TYPE = "acolhimento-anamnese";

    private final OutboxEventPort outboxEventPort;
    private final JsonPayloadSerializer jsonPayloadSerializer;

    public CreateAnamneseAuditOutboxUseCase(
            OutboxEventPort outboxEventPort,
            JsonPayloadSerializer jsonPayloadSerializer) {
        this.outboxEventPort = outboxEventPort;
        this.jsonPayloadSerializer = jsonPayloadSerializer;
    }

    @Transactional
    public void execute(
            HttpEnvelopeDTO<AnamneseDTO> envelope,
            UUID eventId,
            UUID patientId,
            String routingKey,
            AnamneseDTO state) {
        OutboundEnvelopeDTO<AnamneseDTO> auditEnvelope = new OutboundEnvelopeDTO<>(
                eventId,
                envelope.correlationId(),
                ProducerCatalog.PRODUCER_SERVICE,
                ExchangeCatalog.ACOLHIMENTO_EVENT,
                routingKey,
                AGGREGATE_TYPE,
                patientId,
                ProducerCatalog.EVENT_VERSION,
                LocalDateTime.now(),
                envelope.actorId(),
                envelope.userAgent(),
                envelope.originIp(),
                state);

        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .correlationId(envelope.correlationId())
                .producerService(ProducerCatalog.PRODUCER_SERVICE)
                .exchangeName(ExchangeCatalog.ACOLHIMENTO_EVENT)
                .routingKey(routingKey)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(patientId)
                .eventVersion(ProducerCatalog.EVENT_VERSION)
                .payload(jsonPayloadSerializer.serialize(
                        auditEnvelope,
                        envelope.correlationId() != null ? envelope.correlationId().toString() : null,
                        "Falha ao serializar payload de audit outbox de anamnese"))
                .actorId(envelope.actorId())
                .userAgent(envelope.userAgent())
                .originIp(envelope.originIp())
                .status(OutboxStatus.AUDIT)
                .attemptCount(0)
                .maxAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        outboxEventPort.save(event);
    }
}
