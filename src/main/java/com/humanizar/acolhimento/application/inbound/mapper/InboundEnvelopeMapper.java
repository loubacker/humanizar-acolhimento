package com.humanizar.acolhimento.application.inbound.mapper;

import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class InboundEnvelopeMapper {

    public <T> InboundEnvelopeDTO<T> validate(InboundEnvelopeDTO<T> envelop) {
        requireField(envelop, "envelop", null);

        UUID correlationId = requireField(envelop.correlationId(), "envelop.correlationId", null);
        String correlationIdAsString = correlationId.toString();

        requireText(envelop.producerService(), "envelop.producerService", correlationIdAsString);
        requireField(envelop.occurredAt(), "envelop.occurredAt", correlationIdAsString);
        requireField(envelop.actorId(), "envelop.actorId", correlationIdAsString);
        requireText(envelop.userAgent(), "envelop.userAgent", correlationIdAsString);
        requireText(envelop.originIp(), "envelop.originIp", correlationIdAsString);
        requireField(envelop.payload(), "envelop.payload", correlationIdAsString);

        return envelop;
    }

    public String correlationIdAsString(InboundEnvelopeDTO<?> envelop) {
        if (envelop == null || envelop.correlationId() == null) {
            return null;
        }
        return envelop.correlationId().toString();
    }

    public <T> String payloadFieldAsString(InboundEnvelopeDTO<T> envelope, Function<T, ?> extractor) {
        if (envelope == null || envelope.payload() == null) {
            return null;
        }
        Object value = extractor.apply(envelope.payload());
        return value != null ? value.toString() : null;
    }

    private void requireText(String value, String field, String correlationId) {
        if (value == null || value.isBlank()) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_REQUIRED_FIELD,
                    correlationId,
                    "Campo obrigatorio ausente: " + field);
        }
    }

    private <T> T requireField(T value, String field, String correlationId) {
        if (value == null) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_REQUIRED_FIELD,
                    correlationId,
                    "Campo obrigatorio ausente: " + field);
        }
        return value;
    }
}
