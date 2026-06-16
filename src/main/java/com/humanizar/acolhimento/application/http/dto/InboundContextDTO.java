package com.humanizar.acolhimento.application.http.dto;

import java.util.UUID;

import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;

public record InboundContextDTO<T>(
        HttpEnvelopeDTO<T> envelop,
        T payload) {

    public InboundContextDTO {
        if (payload == null && envelop != null) {
            payload = envelop.payload();
        }
    }

    public static <T> InboundContextDTO<T> fromEnvelop(HttpEnvelopeDTO<T> envelop) {
        return new InboundContextDTO<>(envelop, envelop != null ? envelop.payload() : null);
    }

    public String correlationIdText() {
        UUID correlationId = envelop != null ? envelop.correlationId() : null;
        return correlationId != null ? correlationId.toString() : null;
    }
}
