package com.humanizar.acolhimento.application.outbound.mapper;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;

@Component
public class OutboundDeleteMapper {

    private static final String PRODUCER_SERVICE = "humanizar-acolhimento";
    private static final String AGGREGATE_TYPE = "acolhimento";
    private static final short EVENT_VERSION = 1;

    public OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> toDeleteCommandEnvelope(
            InboundEnvelopeDTO<AcolhimentoDeleteDTO> inboundEnvelope,
            UUID eventId,
            UUID aggregateId) {
        Objects.requireNonNull(inboundEnvelope, "inboundEnvelope é obrigatório");
        Objects.requireNonNull(inboundEnvelope.payload(), "inboundEnvelope.payload é obrigatório");
        Objects.requireNonNull(inboundEnvelope.payload().patientId(),
                "inboundEnvelope.payload.patientId é obrigatório");
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(aggregateId, "aggregateId é obrigatório");

        return new OutboundEnvelopeDTO<>(
                eventId,
                inboundEnvelope.correlationId(),
                PRODUCER_SERVICE,
                ExchangeCatalog.ACOLHIMENTO_COMMAND,
                RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2,
                AGGREGATE_TYPE,
                aggregateId,
                EVENT_VERSION,
                LocalDateTime.now(),
                inboundEnvelope.actorId(),
                inboundEnvelope.userAgent(),
                inboundEnvelope.originIp(),
                new AcolhimentoCommandDeletedDTO(inboundEnvelope.payload().patientId()));
    }
}
