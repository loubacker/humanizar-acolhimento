package com.humanizar.acolhimento.application.http.mapper;

import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;

@Component
public class HttpEnvelopeMapper {

    private final InboundFieldValidator fieldValidator;

    public HttpEnvelopeMapper(InboundFieldValidator fieldValidator) {
        this.fieldValidator = fieldValidator;
    }

    public <T> HttpEnvelopeDTO<T> validate(HttpEnvelopeDTO<T> envelop) {
        String correlationIdAsString = validateTracing(envelop);
        fieldValidator.requireField(envelop.payload(), "envelop.payload", correlationIdAsString);
        return envelop;
    }

    public <T> String validateTracing(HttpEnvelopeDTO<T> envelop) {
        fieldValidator.requireField(envelop, "envelop", null);

        UUID correlationId = fieldValidator.requireField(envelop.correlationId(), "envelop.correlationId", null);
        String correlationIdAsString = correlationId.toString();

        fieldValidator.requireText(envelop.producerService(), "envelop.producerService", correlationIdAsString);
        fieldValidator.requireField(envelop.occurredAt(), "envelop.occurredAt", correlationIdAsString);
        fieldValidator.requireField(envelop.actorId(), "envelop.actorId", correlationIdAsString);
        fieldValidator.requireText(envelop.userAgent(), "envelop.userAgent", correlationIdAsString);
        fieldValidator.requireText(envelop.originIp(), "envelop.originIp", correlationIdAsString);

        return correlationIdAsString;
    }

    public String correlationIdAsString(HttpEnvelopeDTO<?> envelop) {
        if (envelop == null || envelop.correlationId() == null) {
            return null;
        }
        return envelop.correlationId().toString();
    }

    public <T> String payloadFieldAsString(HttpEnvelopeDTO<T> envelope, Function<T, ?> extractor) {
        if (envelope == null || envelope.payload() == null) {
            return null;
        }
        Object value = extractor.apply(envelope.payload());
        return value != null ? value.toString() : null;
    }
}
