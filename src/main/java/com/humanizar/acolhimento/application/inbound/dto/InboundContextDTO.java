package com.humanizar.acolhimento.application.inbound.dto;

import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;

public record InboundContextDTO<T>(
        InboundEnvelopeDTO<T> envelop,
        T payload) {

    public InboundContextDTO {
        if (payload == null && envelop != null) {
            payload = envelop.payload();
        }
    }

    public static <T> InboundContextDTO<T> fromEnvelop(InboundEnvelopeDTO<T> envelop) {
        return new InboundContextDTO<>(envelop, envelop != null ? envelop.payload() : null);
    }
}
