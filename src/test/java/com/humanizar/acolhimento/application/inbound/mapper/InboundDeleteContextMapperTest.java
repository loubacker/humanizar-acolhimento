package com.humanizar.acolhimento.application.inbound.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.inbound.dto.InboundDeleteContextDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class InboundDeleteContextMapperTest {

    private final InboundDeleteContextMapper mapper = new InboundDeleteContextMapper(
            new InboundEnvelopeMapper(),
            new InboundAcolhimentoMapper(new NucleoPatientInboundMapper()));

    @Test
    void shouldNormalizeAndValidateDeleteContext() {
        UUID patientId = UUID.randomUUID();
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = validDeleteEnvelope(patientId);

        InboundDeleteContextDTO context = mapper.fromDelete(patientId, envelop);

        assertEquals(envelop, context.envelop());
        assertEquals(patientId, context.payload().patientId());
    }

    @Test
    void shouldFailWhenPathPatientIdDiffersFromPayloadPatientId() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = validDeleteEnvelope(UUID.randomUUID());
        UUID differentPathPatientId = UUID.randomUUID();

        AcolhimentoException thrown = assertThrows(
                AcolhimentoException.class,
                () -> mapper.fromDelete(differentPathPatientId, envelop));

        assertEquals(ReasonCode.INBOUND_PATIENT_MISMATCH, thrown.getReasonCode());
    }

    @Test
    void shouldFailWhenEnvelopeIsMissing() {
        UUID patientId = UUID.randomUUID();

        AcolhimentoException thrown = assertThrows(
                AcolhimentoException.class,
                () -> mapper.fromDelete(patientId, null));

        assertEquals(ReasonCode.INBOUND_REQUIRED_FIELD, thrown.getReasonCode());
    }

    @Test
    void shouldFailWhenPayloadPatientIdIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(null));

        AcolhimentoException thrown = assertThrows(
                AcolhimentoException.class,
                () -> mapper.fromDelete(UUID.randomUUID(), envelop));

        assertEquals(ReasonCode.INBOUND_REQUIRED_FIELD, thrown.getReasonCode());
    }

    private InboundEnvelopeDTO<AcolhimentoDeleteDTO> validDeleteEnvelope(UUID patientId) {
        return new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(patientId));
    }
}
