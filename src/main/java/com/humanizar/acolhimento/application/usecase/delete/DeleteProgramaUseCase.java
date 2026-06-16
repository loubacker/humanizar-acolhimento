package com.humanizar.acolhimento.application.usecase.delete;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.mapper.HttpEnvelopeMapper;
import com.humanizar.acolhimento.application.messaging.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.factory.CommandOutboxEventFactory;
import com.humanizar.acolhimento.application.messaging.outbound.mapper.OutboundDeleteMapper;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@Service
public class DeleteProgramaUseCase {

    private final OutboxEventPort outboxEventPort;
    private final PendingTargetStatusPort pendingTargetStatusPort;
    private final HttpEnvelopeMapper inboundEnvelopeMapper;
    private final OutboundDeleteMapper outboundDeleteMapper;
    private final ObjectMapper objectMapper;
    private final JsonPayloadSerializer jsonPayloadSerializer;
    private final CommandOutboxEventFactory commandOutboxEventFactory;

    public DeleteProgramaUseCase(
            OutboxEventPort outboxEventPort,
            PendingTargetStatusPort pendingTargetStatusPort,
            HttpEnvelopeMapper inboundEnvelopeMapper,
            OutboundDeleteMapper outboundDeleteMapper,
            ObjectMapper objectMapper,
            JsonPayloadSerializer jsonPayloadSerializer,
            CommandOutboxEventFactory commandOutboxEventFactory) {
        this.outboxEventPort = outboxEventPort;
        this.pendingTargetStatusPort = pendingTargetStatusPort;
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
        this.outboundDeleteMapper = outboundDeleteMapper;
        this.objectMapper = objectMapper;
        this.jsonPayloadSerializer = jsonPayloadSerializer;
        this.commandOutboxEventFactory = commandOutboxEventFactory;
    }

    @Transactional
    public void execute(PendingAcolhimento pending) {
        String correlationId = pending.getCorrelationId() != null
                ? pending.getCorrelationId().toString() : null;

        HttpEnvelopeDTO<AcolhimentoDeleteDTO> validatedSourceEnvelope = loadAndValidateSourceDeleteEnvelope(
                pending.getEventId(),
                correlationId);

        UUID aggregateId = pending.getPatientId();

        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> deleteEnvelope = outboundDeleteMapper
                .toDeleteCommandEnvelope(
                        validatedSourceEnvelope,
                        pending.getEventId(),
                        aggregateId);

        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> deleteV1Envelope = deleteEnvelope
                .withRoutingKey(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1);

        OutboxEvent outboxEvent = commandOutboxEventFactory.newCommand(
                UUID.randomUUID(),
                pending.getCorrelationId(),
                RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1,
                aggregateId,
                jsonPayloadSerializer.serialize(
                        deleteV1Envelope,
                        correlationId,
                        "Falha ao serializar payload de command delete para programa no outbox"),
                deleteV1Envelope.actorId(),
                deleteV1Envelope.userAgent(),
                deleteV1Envelope.originIp());
        outboxEventPort.save(outboxEvent);

        pendingTargetStatusPort
                .findByEventIdAndTargetService(pending.getEventId(), TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO)
                .ifPresent(target -> {
                    target.setStatus(Status.PENDING);
                    pendingTargetStatusPort.save(target);
                });
    }

    private HttpEnvelopeDTO<AcolhimentoDeleteDTO> loadAndValidateSourceDeleteEnvelope(
            UUID sourceEventId,
            String correlationId) {
        OutboxEvent sourceEvent = outboxEventPort.findByEventId(sourceEventId)
                .orElseThrow(() -> new AcolhimentoException(
                        ReasonCode.PERSISTENCE_FAILURE,
                        correlationId,
                        "Evento de origem deleted.v2 nao encontrado para gerar deleted.v1"));

        if (!RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2.equals(sourceEvent.getRoutingKey())) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Evento de origem invalido para deleted.v1. routingKey=" + sourceEvent.getRoutingKey());
        }

        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> sourceEnvelope = parseSourceEnvelope(sourceEvent.getPayload(),
                correlationId);
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> inboundEnvelope = getHttpEnvelopeDTO(correlationId, sourceEnvelope);

        try {
            return inboundEnvelopeMapper.validate(inboundEnvelope);
        } catch (AcolhimentoException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Envelope de origem deleted.v2 invalido: " + ex.getMessage());
        }
    }

    private static HttpEnvelopeDTO<AcolhimentoDeleteDTO> getHttpEnvelopeDTO(String correlationId, OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> sourceEnvelope) {
        AcolhimentoCommandDeletedDTO sourcePayload = sourceEnvelope.payload();
        if (sourcePayload == null || sourcePayload.patientId() == null) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Envelope de origem deleted.v2 invalido: payload.patientId ausente");
        }

        return new HttpEnvelopeDTO<>(
                sourceEnvelope.correlationId(),
                sourceEnvelope.producerService(),
                sourceEnvelope.occurredAt(),
                sourceEnvelope.actorId(),
                sourceEnvelope.userAgent(),
                sourceEnvelope.originIp(),
                new AcolhimentoDeleteDTO(sourcePayload.patientId()));
    }

    private OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> parseSourceEnvelope(String payload, String correlationId) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Falha ao desserializar envelope de origem deleted.v2");
        }
    }
}
