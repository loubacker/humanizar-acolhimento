package com.humanizar.acolhimento.application.messaging.outbound.mapper;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.factory.OutboundEnvelopeFactory;

@Component
public class OutboundDeleteMapper {

    private final OutboundEnvelopeFactory outboundEnvelopeFactory;

    public OutboundDeleteMapper(OutboundEnvelopeFactory outboundEnvelopeFactory) {
        this.outboundEnvelopeFactory = outboundEnvelopeFactory;
    }

    public OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> toDeleteCommandEnvelope(
            HttpEnvelopeDTO<AcolhimentoDeleteDTO> inboundEnvelope,
            UUID eventId,
            UUID aggregateId) {
        Objects.requireNonNull(inboundEnvelope, "inboundEnvelope é obrigatório");
        Objects.requireNonNull(inboundEnvelope.payload(), "inboundEnvelope.payload é obrigatório");
        Objects.requireNonNull(inboundEnvelope.payload().patientId(),
                "inboundEnvelope.payload.patientId é obrigatório");
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(aggregateId, "aggregateId é obrigatório");

        return outboundEnvelopeFactory.command(
                inboundEnvelope,
                eventId,
                RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2,
                aggregateId,
                new AcolhimentoCommandDeletedDTO(inboundEnvelope.payload().patientId()));
    }
}
