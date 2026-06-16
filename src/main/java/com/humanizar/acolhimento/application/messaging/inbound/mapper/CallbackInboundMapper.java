package com.humanizar.acolhimento.application.messaging.inbound.mapper;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.http.mapper.InboundFieldValidator;
import com.humanizar.acolhimento.application.messaging.inbound.dto.CallbackDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class CallbackInboundMapper {

    private final ObjectMapper objectMapper;
    private final InboundFieldValidator fieldValidator;

    public CallbackInboundMapper(ObjectMapper objectMapper, InboundFieldValidator fieldValidator) {
        this.objectMapper = objectMapper;
        this.fieldValidator = fieldValidator;
    }

    public CallbackDTO parse(byte[] body) {
        try {
            return objectMapper.readValue(body, CallbackDTO.class);
        } catch (IOException ioException) {
            throw new AcolhimentoException(ReasonCode.INBOUND_PARSE_ERROR, null, "Falha ao parsear callback inbound.");
        }
    }

    public void validate(CallbackDTO callback) {
        fieldValidator.requireField(callback, "callback", null);

        String correlationId = correlationIdAsString(callback);
        fieldValidator.requireText(callback.upStream(), "upStream", correlationId);
        fieldValidator.requireField(callback.eventId(), "eventId", correlationId);
        fieldValidator.requireField(callback.correlationId(), "correlationId", null);
        fieldValidator.requireText(callback.producerService(), "producerService", correlationId);
        fieldValidator.requireText(callback.exchangeName(), "exchangeName", correlationId);
        fieldValidator.requireText(callback.routingKey(), "routingKey", correlationId);
        fieldValidator.requireText(callback.aggregateType(), "aggregateType", correlationId);
        fieldValidator.requireField(callback.aggregateId(), "aggregateId", correlationId);
        fieldValidator.requireField(callback.occurredAt(), "occurredAt", correlationId);
        fieldValidator.requireText(callback.status(), "status", correlationId);
    }

    private String correlationIdAsString(CallbackDTO callback) {
        if (callback == null || callback.correlationId() == null) {
            return null;
        }
        return callback.correlationId().toString();
    }
}
