package com.humanizar.acolhimento.application.http.mapper;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.http.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class InboundContextMapper {

    private final HttpEnvelopeMapper inboundEnvelopeMapper;
    private final InboundFieldValidator fieldValidator;

    public InboundContextMapper(
            HttpEnvelopeMapper inboundEnvelopeMapper,
            InboundFieldValidator fieldValidator) {
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
        this.fieldValidator = fieldValidator;
    }

    public InboundContextDTO<InboundAcolhimentoDTO> normalizeAndValidate(
            InboundContextDTO<InboundAcolhimentoDTO> context) {
        fieldValidator.requireField(context, "context", null);

        HttpEnvelopeDTO<InboundAcolhimentoDTO> envelop = fieldValidator.requireField(context.envelop(),
                "context.envelop", null);
        HttpEnvelopeDTO<InboundAcolhimentoDTO> validatedEnvelope = inboundEnvelopeMapper.validate(envelop);
        String correlationId = inboundEnvelopeMapper.correlationIdAsString(validatedEnvelope);

        InboundAcolhimentoDTO normalizedPayload = resolvePayload(context, validatedEnvelope, correlationId);

        return new InboundContextDTO<>(validatedEnvelope, normalizedPayload);
    }

    public InboundContextDTO<InboundAcolhimentoDTO> normalizeAndValidateUpdate(
            UUID pathPatientId,
            InboundContextDTO<InboundAcolhimentoDTO> context) {
        fieldValidator.requireField(pathPatientId, "path.patientId", null);

        InboundContextDTO<InboundAcolhimentoDTO> normalized = normalizeAndValidate(context);
        String correlationId = inboundEnvelopeMapper.correlationIdAsString(normalized.envelop());

        UUID payloadPatientId = fieldValidator.requireField(
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
            HttpEnvelopeDTO<InboundAcolhimentoDTO> envelop) {
        InboundContextDTO<InboundAcolhimentoDTO> context = InboundContextDTO.fromEnvelop(envelop);
        return normalizeAndValidate(context);
    }

    public InboundContextDTO<InboundAcolhimentoDTO> fromUpdate(
            UUID pathPatientId,
            HttpEnvelopeDTO<InboundAcolhimentoDTO> envelop) {
        InboundContextDTO<InboundAcolhimentoDTO> context = InboundContextDTO.fromEnvelop(envelop);
        return normalizeAndValidateUpdate(pathPatientId, context);
    }

    private InboundAcolhimentoDTO resolvePayload(
            InboundContextDTO<InboundAcolhimentoDTO> context,
            HttpEnvelopeDTO<InboundAcolhimentoDTO> validatedEnvelope,
            String correlationId) {
        InboundAcolhimentoDTO contextPayload = context.payload();
        InboundAcolhimentoDTO envelopePayload = validatedEnvelope.payload();

        if (contextPayload != null && envelopePayload != null && !Objects.equals(contextPayload, envelopePayload)) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_CONTEXT_INCONSISTENT,
                    correlationId,
                    "context.payload diverge de context.envelop.payload");
        }

        InboundAcolhimentoDTO normalizedPayload = contextPayload != null ? contextPayload : envelopePayload;
        fieldValidator.requireField(normalizedPayload, "context.payload", correlationId);
        return normalizedPayload;
    }
}
