package com.humanizar.acolhimento.application.usecase.delete;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.factory.CommandOutboxEventFactory;
import com.humanizar.acolhimento.application.messaging.outbound.mapper.OutboundDeleteMapper;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@Service
public class DeleteOutboxCommandUseCase {

    private final OutboxEventPort outboxEventPort;
    private final OutboundDeleteMapper outboundDeleteMapper;
    private final JsonPayloadSerializer jsonPayloadSerializer;
    private final CommandOutboxEventFactory commandOutboxEventFactory;

    public DeleteOutboxCommandUseCase(
            OutboxEventPort outboxEventPort,
            OutboundDeleteMapper outboundDeleteMapper,
            JsonPayloadSerializer jsonPayloadSerializer,
            CommandOutboxEventFactory commandOutboxEventFactory) {
        this.outboxEventPort = outboxEventPort;
        this.outboundDeleteMapper = outboundDeleteMapper;
        this.jsonPayloadSerializer = jsonPayloadSerializer;
        this.commandOutboxEventFactory = commandOutboxEventFactory;
    }

    @Transactional
    public void execute(
            HttpEnvelopeDTO<AcolhimentoDeleteDTO> inboundEnvelope,
            UUID eventId,
            UUID aggregateId) {
        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> envelope = outboundDeleteMapper
                .toDeleteCommandEnvelope(inboundEnvelope, eventId, aggregateId);

        String correlationIdText = inboundEnvelope.correlationId() != null
                ? inboundEnvelope.correlationId().toString() : null;

        OutboxEvent outboxEvent = commandOutboxEventFactory.newCommand(
                eventId,
                inboundEnvelope.correlationId(),
                RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2,
                aggregateId,
                jsonPayloadSerializer.serialize(
                        envelope,
                        correlationIdText,
                        "Falha ao serializar payload de command delete no outbox"),
                inboundEnvelope.actorId(),
                inboundEnvelope.userAgent(),
                inboundEnvelope.originIp());

        outboxEventPort.save(outboxEvent);
    }
}
