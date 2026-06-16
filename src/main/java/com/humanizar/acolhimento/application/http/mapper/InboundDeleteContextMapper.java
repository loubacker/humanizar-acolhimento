package com.humanizar.acolhimento.application.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.http.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class InboundDeleteContextMapper {

    private final HttpEnvelopeMapper inboundEnvelopeMapper;
    private final InboundAcolhimentoMapper inboundAcolhimentoMapper;
    private final InboundFieldValidator fieldValidator;

    public InboundDeleteContextMapper(
            HttpEnvelopeMapper inboundEnvelopeMapper,
            InboundAcolhimentoMapper inboundAcolhimentoMapper,
            InboundFieldValidator fieldValidator) {
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
        this.inboundAcolhimentoMapper = inboundAcolhimentoMapper;
        this.fieldValidator = fieldValidator;
    }

    public InboundContextDTO<AcolhimentoDeleteDTO> fromDelete(
            UUID pathPatientId,
            HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop) {
        fieldValidator.requireField(pathPatientId, "path.patientId", null);

        HttpEnvelopeDTO<AcolhimentoDeleteDTO> validatedEnvelope = inboundEnvelopeMapper.validate(envelop);
        String correlationId = inboundEnvelopeMapper.correlationIdAsString(validatedEnvelope);

        AcolhimentoDeleteDTO payload = inboundAcolhimentoMapper.toDeletePayload(validatedEnvelope.payload());
        UUID payloadPatientId = fieldValidator.requireField(payload.patientId(), "context.payload.patientId",
                correlationId);

        if (!pathPatientId.equals(payloadPatientId)) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_PATIENT_MISMATCH,
                    correlationId,
                    "path.patientId diverge de payload.patientId");
        }

        return new InboundContextDTO<>(validatedEnvelope, payload);
    }
}
