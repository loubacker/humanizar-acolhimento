package com.humanizar.acolhimento.application.inbound.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.inbound.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class InboundContextMapperTest {

    private final InboundContextMapper mapper = new InboundContextMapper(new InboundEnvelopeMapper());

    @Test
    void shouldInheritPayloadFromEnvelop() {
        InboundAcolhimentoDTO payload = validPayload(UUID.randomUUID());
        InboundEnvelopeDTO<InboundAcolhimentoDTO> envelop = validEnvelop(payload);

        InboundContextDTO<InboundAcolhimentoDTO> context = mapper.fromEnvelop(envelop);

        assertNotNull(context);
        assertEquals(envelop, context.envelop());
        assertEquals(payload, context.payload());
    }

    @Test
    void shouldFailWhenContextPayloadDiffersFromEnvelopPayload() {
        UUID patientId = UUID.randomUUID();
        InboundAcolhimentoDTO payloadA = validPayload(patientId);
        InboundAcolhimentoDTO payloadB = new InboundAcolhimentoDTO(
                patientId,
                LocalDate.now().plusDays(1),
                LocalTime.NOON,
                "outro payload",
                payloadA.nucleoPatient());

        InboundEnvelopeDTO<InboundAcolhimentoDTO> envelop = validEnvelop(payloadA);
        InboundContextDTO<InboundAcolhimentoDTO> context = new InboundContextDTO<>(envelop, payloadB);

        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> mapper.normalizeAndValidate(context));

        assertEquals(ReasonCode.INBOUND_CONTEXT_INCONSISTENT, exception.getReasonCode());
    }

    @Test
    void shouldFailWhenPathPatientIdDiffersFromPayloadPatientId() {
        UUID payloadPatientId = UUID.randomUUID();
        UUID pathPatientId = UUID.randomUUID();
        InboundAcolhimentoDTO payload = validPayload(payloadPatientId);

        InboundEnvelopeDTO<InboundAcolhimentoDTO> envelop = validEnvelop(payload);
        InboundContextDTO<InboundAcolhimentoDTO> context = InboundContextDTO.fromEnvelop(envelop);

        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> mapper.normalizeAndValidateUpdate(pathPatientId, context));

        assertEquals(ReasonCode.INBOUND_PATIENT_MISMATCH, exception.getReasonCode());
    }

    @Test
    void shouldValidateUpdateWhenPathAndPayloadPatientIdMatch() {
        UUID patientId = UUID.randomUUID();
        InboundAcolhimentoDTO payload = validPayload(patientId);

        InboundContextDTO<InboundAcolhimentoDTO> context = mapper.fromUpdate(patientId, validEnvelop(payload));

        assertEquals(patientId, context.payload().patientId());
        assertEquals(patientId, context.envelop().payload().patientId());
    }

    private InboundEnvelopeDTO<InboundAcolhimentoDTO> validEnvelop(InboundAcolhimentoDTO payload) {
        return new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                payload);
    }

    private InboundAcolhimentoDTO validPayload(UUID patientId) {
        return new InboundAcolhimentoDTO(
                patientId,
                LocalDate.now(),
                LocalTime.of(9, 30),
                "observacao",
                List.of(new NucleoPatientDTO(
                        UUID.randomUUID(),
                        List.of(new NucleoResponsavelDTO(UUID.randomUUID(), "COORDENADOR")))));
    }
}
