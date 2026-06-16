package com.humanizar.acolhimento.application.http.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class InboundFieldValidator {

    public <T> T requireField(T value, String field, String correlationId) {
        if (value == null) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_REQUIRED_FIELD,
                    correlationId,
                    "Campo obrigatorio ausente: " + field);
        }
        return value;
    }

    public String requireText(String value, String field, String correlationId) {
        if (value == null || value.isBlank()) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_REQUIRED_FIELD,
                    correlationId,
                    "Campo obrigatorio ausente: " + field);
        }
        return value;
    }

    public <T> List<T> requireNonEmpty(List<T> value, String field, String correlationId) {
        if (value == null || value.isEmpty()) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_EMPTY_COLLECTION,
                    correlationId,
                    "Colecao obrigatoria vazia: " + field);
        }
        return value;
    }
}
