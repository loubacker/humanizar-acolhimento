package com.humanizar.acolhimento.application.messaging.inbound.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.http.mapper.InboundFieldValidator;
import com.humanizar.acolhimento.application.messaging.inbound.dto.CallbackDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.config.ObjectMapperConfig;

class CallbackInboundMapperTest {

    private static final String UP_STREAM = "cmd.acolhimento.created.v1";
    private static final String PRODUCER = "humanizar-nucleo-relacionamento";
    private static final String EXCHANGE = "humanizar.acolhimento.event";
    private static final String ROUTING_KEY = "ev.acolhimento.nucleo-relacionamento.processed.v1";
    private static final String AGGREGATE_TYPE = "acolhimento";
    private static final String STATUS = "PROCESSED";

    private ObjectMapper objectMapper;
    private CallbackInboundMapper callbackInboundMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperConfig().objectMapper();
        callbackInboundMapper = new CallbackInboundMapper(objectMapper, new InboundFieldValidator());
    }

    @Test
    void shouldParseCallbackRoundTrip() throws Exception {
        CallbackDTO original = validCallback();
        byte[] body = objectMapper.writeValueAsBytes(original);

        CallbackDTO parsed = callbackInboundMapper.parse(body);

        assertEquals(original, parsed);
    }

    @Test
    void shouldThrowInboundParseErrorOnInvalidJson() {
        byte[] invalid = "{invalid".getBytes();

        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> callbackInboundMapper.parse(invalid));

        assertEquals(ReasonCode.INBOUND_PARSE_ERROR, exception.getReasonCode());
    }

    @Test
    void shouldValidateValidCallback() {
        assertDoesNotThrow(() -> callbackInboundMapper.validate(validCallback()));
    }

    @Test
    void shouldRejectNullCallback() {
        assertRequiredField(null, "callback");
    }

    @Test
    void shouldRejectBlankUpStream() {
        assertRequiredField(
                callback("  ", UUID.randomUUID(), UUID.randomUUID(), PRODUCER, EXCHANGE, ROUTING_KEY,
                        AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), STATUS),
                "upStream");
    }

    @Test
    void shouldRejectNullEventId() {
        assertRequiredField(
                callback(UP_STREAM, null, UUID.randomUUID(), PRODUCER, EXCHANGE, ROUTING_KEY,
                        AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), STATUS),
                "eventId");
    }

    @Test
    void shouldRejectNullCorrelationId() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), null, PRODUCER, EXCHANGE, ROUTING_KEY,
                        AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), STATUS),
                "correlationId");
    }

    @Test
    void shouldRejectBlankProducerService() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), "  ", EXCHANGE, ROUTING_KEY,
                        AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), STATUS),
                "producerService");
    }

    @Test
    void shouldRejectBlankExchangeName() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), PRODUCER, "  ", ROUTING_KEY,
                        AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), STATUS),
                "exchangeName");
    }

    @Test
    void shouldRejectBlankRoutingKey() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), PRODUCER, EXCHANGE, "  ",
                        AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), STATUS),
                "routingKey");
    }

    @Test
    void shouldRejectBlankAggregateType() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), PRODUCER, EXCHANGE, ROUTING_KEY,
                        "  ", UUID.randomUUID(), LocalDateTime.now(), STATUS),
                "aggregateType");
    }

    @Test
    void shouldRejectNullAggregateId() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), PRODUCER, EXCHANGE, ROUTING_KEY,
                        AGGREGATE_TYPE, null, LocalDateTime.now(), STATUS),
                "aggregateId");
    }

    @Test
    void shouldRejectNullOccurredAt() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), PRODUCER, EXCHANGE, ROUTING_KEY,
                        AGGREGATE_TYPE, UUID.randomUUID(), null, STATUS),
                "occurredAt");
    }

    @Test
    void shouldRejectBlankStatus() {
        assertRequiredField(
                callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), PRODUCER, EXCHANGE, ROUTING_KEY,
                        AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), "  "),
                "status");
    }

    private void assertRequiredField(CallbackDTO callback, String fieldName) {
        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> callbackInboundMapper.validate(callback));

        assertEquals(ReasonCode.INBOUND_REQUIRED_FIELD, exception.getReasonCode());
        assertTrue(exception.getMessage().contains(fieldName));
    }

    private CallbackDTO validCallback() {
        return callback(UP_STREAM, UUID.randomUUID(), UUID.randomUUID(), PRODUCER, EXCHANGE, ROUTING_KEY,
                AGGREGATE_TYPE, UUID.randomUUID(), LocalDateTime.now(), STATUS);
    }

    private CallbackDTO callback(
            String upStream,
            UUID eventId,
            UUID correlationId,
            String producerService,
            String exchangeName,
            String routingKey,
            String aggregateType,
            UUID aggregateId,
            LocalDateTime occurredAt,
            String status) {
        return new CallbackDTO(
                upStream,
                eventId,
                correlationId,
                producerService,
                exchangeName,
                routingKey,
                aggregateType,
                aggregateId,
                1,
                occurredAt,
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                status,
                null,
                null,
                LocalDateTime.now(),
                null);
    }
}
