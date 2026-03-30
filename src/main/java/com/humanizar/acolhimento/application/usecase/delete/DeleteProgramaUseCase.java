package com.humanizar.acolhimento.application.usecase.delete;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.inbound.mapper.InboundEnvelopeMapper;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.mapper.OutboundDeleteMapper;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveAcolhimentoUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@Service
public class DeleteProgramaUseCase {

    private static final String PRODUCER_SERVICE = "humanizar-acolhimento";
    private static final String AGGREGATE_TYPE = "acolhimento";
    private static final short EVENT_VERSION = 1;
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final OutboxEventPort outboxEventPort;
    private final PendingTargetStatusPort pendingTargetStatusPort;
    private final RetrieveAcolhimentoUseCase retrieveAcolhimentoUseCase;
    private final InboundEnvelopeMapper inboundEnvelopeMapper;
    private final OutboundDeleteMapper outboundDeleteMapper;
    private final ObjectMapper objectMapper;

    public DeleteProgramaUseCase(
            OutboxEventPort outboxEventPort,
            PendingTargetStatusPort pendingTargetStatusPort,
            RetrieveAcolhimentoUseCase retrieveAcolhimentoUseCase,
            InboundEnvelopeMapper inboundEnvelopeMapper,
            OutboundDeleteMapper outboundDeleteMapper,
            ObjectMapper objectMapper) {
        this.outboxEventPort = outboxEventPort;
        this.pendingTargetStatusPort = pendingTargetStatusPort;
        this.retrieveAcolhimentoUseCase = retrieveAcolhimentoUseCase;
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
        this.outboundDeleteMapper = outboundDeleteMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(PendingAcolhimento pending) {
        String correlationId = pending.getCorrelationId() != null
                ? pending.getCorrelationId().toString() : null;

        InboundEnvelopeDTO<AcolhimentoDeleteDTO> validatedSourceEnvelope = loadAndValidateSourceDeleteEnvelope(
                pending.getEventId(),
                correlationId);

        Acolhimento acolhimento = retrieveAcolhimentoUseCase
                .execute(pending.getPatientId(), correlationId);

        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> deleteEnvelope = outboundDeleteMapper
                .toDeleteCommandEnvelope(
                        validatedSourceEnvelope,
                        pending.getEventId(),
                        acolhimento.getId());

        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> deleteV1Envelope = new OutboundEnvelopeDTO<>(
                deleteEnvelope.eventId(),
                deleteEnvelope.correlationId(),
                deleteEnvelope.producerService(),
                deleteEnvelope.exchangeName(),
                RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1,
                deleteEnvelope.aggregateType(),
                deleteEnvelope.aggregateId(),
                deleteEnvelope.eventVersion(),
                deleteEnvelope.occurredAt(),
                deleteEnvelope.actorId(),
                deleteEnvelope.userAgent(),
                deleteEnvelope.originIp(),
                deleteEnvelope.payload());

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .correlationId(pending.getCorrelationId())
                .producerService(PRODUCER_SERVICE)
                .exchangeName(ExchangeCatalog.ACOLHIMENTO_COMMAND)
                .routingKey(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(acolhimento.getId())
                .eventVersion(EVENT_VERSION)
                .payload(serialize(deleteV1Envelope, correlationId))
                .actorId(deleteV1Envelope.actorId())
                .userAgent(deleteV1Envelope.userAgent())
                .originIp(deleteV1Envelope.originIp())
                .status(OutboxStatus.NEW)
                .attemptCount(0)
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .nextRetryAt(LocalDateTime.now())
                .build();
        outboxEventPort.save(outboxEvent);

        pendingTargetStatusPort
                .findByEventIdAndTargetService(pending.getEventId(), TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO)
                .ifPresent(target -> {
                    target.setStatus(Status.PENDING);
                    pendingTargetStatusPort.save(target);
                });
    }

    private InboundEnvelopeDTO<AcolhimentoDeleteDTO> loadAndValidateSourceDeleteEnvelope(
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
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> inboundEnvelope = getInboundEnvelopeDTO(correlationId, sourceEnvelope);

        try {
            return inboundEnvelopeMapper.validate(inboundEnvelope);
        } catch (AcolhimentoException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Envelope de origem deleted.v2 invalido: " + ex.getMessage());
        }
    }

    private static InboundEnvelopeDTO<AcolhimentoDeleteDTO> getInboundEnvelopeDTO(String correlationId, OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> sourceEnvelope) {
        AcolhimentoCommandDeletedDTO sourcePayload = sourceEnvelope.payload();
        if (sourcePayload == null || sourcePayload.patientId() == null) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Envelope de origem deleted.v2 invalido: payload.patientId ausente");
        }

        return new InboundEnvelopeDTO<>(
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

    private String serialize(Object value, String correlationId) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Falha ao serializar payload de command delete para programa no outbox");
        }
    }
}
