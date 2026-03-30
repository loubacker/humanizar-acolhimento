package com.humanizar.acolhimento.application.usecase.delete;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.mapper.OutboundDeleteMapper;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@Service
public class DeleteOutboxCommandUseCase {

    private static final String PRODUCER_SERVICE = "humanizar-acolhimento";
    private static final String AGGREGATE_TYPE = "acolhimento";
    private static final short EVENT_VERSION = 1;
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final OutboxEventPort outboxEventPort;
    private final OutboundDeleteMapper outboundDeleteMapper;
    private final ObjectMapper objectMapper;

    public DeleteOutboxCommandUseCase(
            OutboxEventPort outboxEventPort,
            OutboundDeleteMapper outboundDeleteMapper,
            ObjectMapper objectMapper) {
        this.outboxEventPort = outboxEventPort;
        this.outboundDeleteMapper = outboundDeleteMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(
            InboundEnvelopeDTO<AcolhimentoDeleteDTO> inboundEnvelope,
            UUID eventId,
            UUID aggregateId) {
        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> envelope = outboundDeleteMapper
                .toDeleteCommandEnvelope(inboundEnvelope, eventId, aggregateId);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .correlationId(inboundEnvelope.correlationId())
                .producerService(PRODUCER_SERVICE)
                .exchangeName(ExchangeCatalog.ACOLHIMENTO_COMMAND)
                .routingKey(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .eventVersion(EVENT_VERSION)
                .payload(serialize(envelope, inboundEnvelope.correlationId()))
                .actorId(inboundEnvelope.actorId())
                .userAgent(inboundEnvelope.userAgent())
                .originIp(inboundEnvelope.originIp())
                .status(OutboxStatus.NEW)
                .attemptCount(0)
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .nextRetryAt(LocalDateTime.now())
                .build();

        outboxEventPort.save(outboxEvent);
    }

    private String serialize(Object value, UUID correlationId) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId != null ? correlationId.toString() : null,
                    "Falha ao serializar payload de command delete no outbox");
        }
    }
}
