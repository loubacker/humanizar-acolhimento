package com.humanizar.acolhimento.application.http.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.http.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class InboundDeleteContextMapperTest {

    private final InboundFieldValidator fieldValidator = new InboundFieldValidator();
    private final InboundDeleteContextMapper mapper = new InboundDeleteContextMapper(
            new HttpEnvelopeMapper(fieldValidator),
            new InboundAcolhimentoMapper(new NucleoPatientInboundMapper(fieldValidator), fieldValidator),
            fieldValidator);

    @Test
    void shouldNormalizeAndValidateDeleteContext() {
        UUID patientId = UUID.randomUUID();
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = validDeleteEnvelope(patientId);

        InboundContextDTO<AcolhimentoDeleteDTO> context = mapper.fromDelete(patientId, envelop);

        assertEquals(envelop, context.envelop());
        assertEquals(patientId, context.payload().patientId());
    }

    @Test
    void shouldFailWhenPathPatientIdDiffersFromPayloadPatientId() {
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = validDeleteEnvelope(UUID.randomUUID());
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
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
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

    private HttpEnvelopeDTO<AcolhimentoDeleteDTO> validDeleteEnvelope(UUID patientId) {
        return new HttpEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(patientId));
    }
}
