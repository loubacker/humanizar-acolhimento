package com.humanizar.acolhimento.application.usecase.create;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoMappingResult;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.mapper.OutboundCreateMapper;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@Service
public class CreateOutboxCommandUseCase {

    private static final String PRODUCER_SERVICE = "humanizar-acolhimento";
    private static final String AGGREGATE_TYPE = "acolhimento";
    private static final short EVENT_VERSION = 1;
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final OutboxEventPort outboxEventPort;
    private final OutboundCreateMapper createOutboundMapper;
    private final ObjectMapper objectMapper;

    public CreateOutboxCommandUseCase(
            OutboxEventPort outboxEventPort,
            OutboundCreateMapper createOutboundMapper,
            ObjectMapper objectMapper) {
        this.outboxEventPort = outboxEventPort;
        this.createOutboundMapper = createOutboundMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(
            InboundEnvelopeDTO<InboundAcolhimentoDTO> inboundEnvelope,
            UUID eventId,
            InboundAcolhimentoMappingResult mappingResult) {
        OutboundEnvelopeDTO<AcolhimentoCommandDTO> outboundEnvelope = createOutboundMapper
                .toCreateCommandEnvelope(inboundEnvelope, eventId, mappingResult);

        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .correlationId(inboundEnvelope.correlationId())
                .producerService(PRODUCER_SERVICE)
                .exchangeName(ExchangeCatalog.ACOLHIMENTO_COMMAND)
                .routingKey(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(mappingResult.acolhimento().getId())
                .eventVersion(EVENT_VERSION)
                .payload(serialize(outboundEnvelope, inboundEnvelope))
                .actorId(inboundEnvelope.actorId())
                .userAgent(inboundEnvelope.userAgent())
                .originIp(inboundEnvelope.originIp())
                .status(OutboxStatus.NEW)
                .attemptCount(0)
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .nextRetryAt(LocalDateTime.now())
                .build();

        outboxEventPort.save(event);
    }

    private String serialize(Object value, InboundEnvelopeDTO<InboundAcolhimentoDTO> inboundEnvelope) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    inboundEnvelope.correlationId() != null ? inboundEnvelope.correlationId().toString() : null,
                    "Falha ao serializar payload de command outbox");
        }
    }
}
