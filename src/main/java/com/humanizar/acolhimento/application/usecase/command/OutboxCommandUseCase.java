package com.humanizar.acolhimento.application.usecase.command;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoMapResultDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.AcolhimentoCommandDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.factory.CommandOutboxEventFactory;
import com.humanizar.acolhimento.application.messaging.outbound.mapper.OutboundUpsertMapper;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@Service
public class OutboxCommandUseCase {

    private final OutboxEventPort outboxEventPort;
    private final OutboundUpsertMapper outboundUpsertMapper;
    private final JsonPayloadSerializer jsonPayloadSerializer;
    private final CommandOutboxEventFactory commandOutboxEventFactory;

    public OutboxCommandUseCase(
            OutboxEventPort outboxEventPort,
            OutboundUpsertMapper outboundUpsertMapper,
            JsonPayloadSerializer jsonPayloadSerializer,
            CommandOutboxEventFactory commandOutboxEventFactory) {
        this.outboxEventPort = outboxEventPort;
        this.outboundUpsertMapper = outboundUpsertMapper;
        this.jsonPayloadSerializer = jsonPayloadSerializer;
        this.commandOutboxEventFactory = commandOutboxEventFactory;
    }

    @Transactional
    public void execute(
            HttpEnvelopeDTO<InboundAcolhimentoDTO> inboundEnvelope,
            UUID eventId,
            InboundAcolhimentoMapResultDTO mappingResult,
            String routingKey) {
        OutboundEnvelopeDTO<AcolhimentoCommandDTO> outboundEnvelope = outboundUpsertMapper
                .toCommandEnvelope(inboundEnvelope, eventId, mappingResult, routingKey);

        String correlationIdText = inboundEnvelope.correlationId() != null
                ? inboundEnvelope.correlationId().toString() : null;

        OutboxEvent event = commandOutboxEventFactory.newCommand(
                eventId,
                inboundEnvelope.correlationId(),
                routingKey,
                mappingResult.patientId(),
                jsonPayloadSerializer.serialize(
                        outboundEnvelope,
                        correlationIdText,
                        "Falha ao serializar payload de command outbox"),
                inboundEnvelope.actorId(),
                inboundEnvelope.userAgent(),
                inboundEnvelope.originIp());

        outboxEventPort.save(event);
    }
}
