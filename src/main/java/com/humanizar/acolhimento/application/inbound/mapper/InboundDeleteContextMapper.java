package com.humanizar.acolhimento.application.inbound.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.inbound.dto.InboundDeleteContextDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class InboundDeleteContextMapper {

    private final InboundEnvelopeMapper inboundEnvelopeMapper;
    private final InboundAcolhimentoMapper inboundAcolhimentoMapper;

    public InboundDeleteContextMapper(
            InboundEnvelopeMapper inboundEnvelopeMapper,
            InboundAcolhimentoMapper inboundAcolhimentoMapper) {
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
        this.inboundAcolhimentoMapper = inboundAcolhimentoMapper;
    }

    public InboundDeleteContextDTO fromDelete(
            UUID pathPatientId,
            InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop) {
        requireField(pathPatientId, "path.patientId", null);

        InboundEnvelopeDTO<AcolhimentoDeleteDTO> validatedEnvelope = inboundEnvelopeMapper.validate(envelop);
        String correlationId = inboundEnvelopeMapper.correlationIdAsString(validatedEnvelope);

        AcolhimentoDeleteDTO payload = inboundAcolhimentoMapper.toDeletePayload(validatedEnvelope.payload());
        UUID payloadPatientId = requireField(payload.patientId(), "context.payload.patientId", correlationId);

        if (!pathPatientId.equals(payloadPatientId)) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_PATIENT_MISMATCH,
                    correlationId,
                    "path.patientId diverge de payload.patientId");
        }

        return new InboundDeleteContextDTO(validatedEnvelope, payload);
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
