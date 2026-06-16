package com.humanizar.acolhimento.application.http.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class HttpEnvelopeMapperTest {

    private final HttpEnvelopeMapper mapper = new HttpEnvelopeMapper(new InboundFieldValidator());

    @Test
    void shouldValidateEnvelopeSuccessfully() {
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = validEnvelop();

        assertDoesNotThrow(() -> mapper.validate(envelop));
        assertEquals(envelop.correlationId().toString(), mapper.correlationIdAsString(envelop));
        assertEquals(envelop, mapper.validate(envelop));
    }

    @Test
    void shouldFailWhenCorrelationIdIsMissing() {
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
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
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
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
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
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
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
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
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
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
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
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
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                null);

        assertReasonCode(envelop, ReasonCode.INBOUND_REQUIRED_FIELD);
    }

    private void assertReasonCode(HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop, ReasonCode reasonCode) {
        AcolhimentoException exception = assertThrows(AcolhimentoException.class, () -> mapper.validate(envelop));
        assertEquals(reasonCode, exception.getReasonCode());
    }

    private HttpEnvelopeDTO<AcolhimentoDeleteDTO> validEnvelop() {
        return new HttpEnvelopeDTO<>(
                UUID.randomUUID(),
                "humanizar-gateway",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                new AcolhimentoDeleteDTO(UUID.randomUUID()));
    }
}
