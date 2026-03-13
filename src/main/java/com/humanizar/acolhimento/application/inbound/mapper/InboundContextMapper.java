package com.humanizar.acolhimento.application.inbound.mapper;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.inbound.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class InboundContextMapper {

    private final InboundEnvelopeMapper inboundEnvelopeMapper;

    public InboundContextMapper(InboundEnvelopeMapper inboundEnvelopeMapper) {
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
    }

    public InboundContextDTO<InboundAcolhimentoDTO> normalizeAndValidate(
            InboundContextDTO<InboundAcolhimentoDTO> context) {
        requireField(context, "context", null);

        InboundEnvelopeDTO<InboundAcolhimentoDTO> envelop = requireField(context.envelop(), "context.envelop", null);
        InboundEnvelopeDTO<InboundAcolhimentoDTO> validatedEnvelope = inboundEnvelopeMapper.validate(envelop);

        String correlationId = inboundEnvelopeMapper.correlationIdAsString(validatedEnvelope);
        InboundAcolhimentoDTO contextPayload = context.payload();
        InboundAcolhimentoDTO envelopePayload = validatedEnvelope.payload();

        if (contextPayload != null && envelopePayload != null && !Objects.equals(contextPayload, envelopePayload)) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_CONTEXT_INCONSISTENT,
                    correlationId,
                    "context.payload diverge de context.envelop.payload");
        }

        InboundAcolhimentoDTO normalizedPayload = contextPayload != null ? contextPayload : envelopePayload;
        requireField(normalizedPayload, "context.payload", correlationId);

        return new InboundContextDTO<>(validatedEnvelope, normalizedPayload);
    }

    public InboundContextDTO<InboundAcolhimentoDTO> normalizeAndValidateUpdate(
            UUID pathPatientId,
            InboundContextDTO<InboundAcolhimentoDTO> context) {
        requireField(pathPatientId, "path.patientId", null);

        InboundContextDTO<InboundAcolhimentoDTO> normalized = normalizeAndValidate(context);
        String correlationId = inboundEnvelopeMapper.correlationIdAsString(normalized.envelop());

        UUID payloadPatientId = requireField(
                normalized.payload().patientId(),
                "context.payload.patientId",
                correlationId);

        if (!pathPatientId.equals(payloadPatientId)) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_PATIENT_MISMATCH,
                    correlationId,
                    "path.patientId diverge de payload.patientId");
        }

        return normalized;
    }

    public InboundContextDTO<InboundAcolhimentoDTO> fromEnvelop(
            InboundEnvelopeDTO<InboundAcolhimentoDTO> envelop) {
        InboundContextDTO<InboundAcolhimentoDTO> context = InboundContextDTO.fromEnvelop(envelop);
        return normalizeAndValidate(context);
    }

    public InboundContextDTO<InboundAcolhimentoDTO> fromUpdate(
            UUID pathPatientId,
            InboundEnvelopeDTO<InboundAcolhimentoDTO> envelop) {
        InboundContextDTO<InboundAcolhimentoDTO> context = InboundContextDTO.fromEnvelop(envelop);
        return normalizeAndValidateUpdate(pathPatientId, context);
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
