package com.humanizar.acolhimento.application.inbound.dto;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;

public record InboundDeleteContextDTO(
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop,
        AcolhimentoDeleteDTO payload) {

    public InboundDeleteContextDTO {
        if (payload == null && envelop != null) {
            payload = envelop.payload();
        }
    }
}
