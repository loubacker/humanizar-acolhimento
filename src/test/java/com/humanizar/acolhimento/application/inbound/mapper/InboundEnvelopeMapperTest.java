package com.humanizar.acolhimento.application.inbound.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class InboundEnvelopeMapperTest {

    private final InboundEnvelopeMapper mapper = new InboundEnvelopeMapper();

    @Test
    void shouldValidateEnvelopeSuccessfully() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = validEnvelop();

        assertDoesNotThrow(() -> mapper.validate(envelop));
        assertEquals(envelop.correlationId().toString(), mapper.correlationIdAsString(envelop));
        assertEquals(envelop, mapper.toInboundCommand(envelop));
    }

    @Test
    void shouldFailWhenCorrelationIdIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                null,
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    @Test
    void shouldFailWhenProducerServiceIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "  ",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    @Test
    void shouldFailWhenOccurredAtIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                null,
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    @Test
    void shouldFailWhenActorIdIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                null,
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    @Test
    void shouldFailWhenUserAgentIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    @Test
    void shouldFailWhenOriginIpIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                " ",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    @Test
    void shouldFailWhenPayloadIsMissing() {
        InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                null);

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    private void assertReasonCode(InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop, ReasonCode reasonCode) {
        AcolhimentoException exception = assertThrows(AcolhimentoException.class, () -> mapper.validate(envelop));
        assertEquals(reasonCode, exception.getReasonCode());
    }

    private InboundEnvelopeDTO<AcolhimentoDeleteDTO> validEnvelop() {
        return new InboundEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));
    }
}
