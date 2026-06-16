package com.humanizar.acolhimento.application.serializer;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class JsonPayloadSerializer {

    private final ObjectMapper objectMapper;

    public JsonPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object value, String correlationId, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    errorMessage);
        }
    }
}
