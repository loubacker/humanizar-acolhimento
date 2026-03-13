package com.humanizar.acolhimento.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.outbound.CallbackDTO;
import com.humanizar.acolhimento.infrastructure.config.ObjectMapperConfig;

class ProcessingResultCallbackDTODeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        objectMapper = new ObjectMapperConfig().objectMapper();
    }

    @Test
    void shouldDeserializeProcessedCallbackWithOptionalFieldsAsNull() throws Exception {
        CallbackDTO callback = objectMapper.readValue(processedJson(), CallbackDTO.class);

        assertEquals("ev.acolhimento.created.v1", callback.upStream());
        assertEquals(UUID.fromString("7b736ee4-7fc4-48dc-ae60-01050f660f9f"), callback.eventId());
        assertEquals("PROCESSED", callback.status());
        assertEquals(LocalDateTime.of(2026, 3, 2, 15, 0), callback.processedAt());
        assertNull(callback.reasonCode());
        assertNull(callback.errorMessage());
        assertNull(callback.rejectedAt());
    }

    @Test
    void shouldDeserializeRejectedCallbackWithReasonAndRejectedAt() throws Exception {
        CallbackDTO callback = objectMapper.readValue(rejectedJson(), CallbackDTO.class);

        assertEquals("ev.acolhimento.updated.v1", callback.upStream());
        assertEquals(UUID.fromString("8e976f4d-92da-4cd6-a636-b66f3e4a4f3e"), callback.eventId());
        assertEquals("REJECTED", callback.status());
        assertEquals("VALIDATION_ERROR", callback.reasonCode());
        assertEquals("Falha de validacao do payload/evento.", callback.errorMessage());
        assertEquals(LocalDateTime.of(2026, 3, 2, 15, 10), callback.rejectedAt());
        assertNull(callback.processedAt());
    }

    private String processedJson() {
        return """
                {
                  "upStream": "ev.acolhimento.created.v1",
                  "eventId": "7b736ee4-7fc4-48dc-ae60-01050f660f9f",
                  "correlationId": "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
                  "producerService": "humanizar-nucleo-relacionamento",
                  "exchangeName": "humanizar.acolhimento.event",
                  "routingKey": "ev.nucleo-relacionamento.acolhimento.processed.v1",
                  "aggregateType": "acolhimento",
                  "aggregateId": "8206e539-fc43-42f8-8bd5-bf6164b835dc",
                  "eventVersion": 1,
                  "occurredAt": "2026-03-02T15:00:00",
                  "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
                  "userAgent": "Mozilla/5.0",
                  "originIp": "187.11.22.33",
                  "status": "PROCESSED",
                  "processedAt": "2026-03-02T15:00:00"
                }
                """;
    }

    private String rejectedJson() {
        return """
                {
                  "upStream": "ev.acolhimento.updated.v1",
                  "eventId": "8e976f4d-92da-4cd6-a636-b66f3e4a4f3e",
                  "correlationId": "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
                  "producerService": "humanizar-nucleo-relacionamento",
                  "exchangeName": "humanizar.acolhimento.event",
                  "routingKey": "ev.nucleo-relacionamento.acolhimento.rejected.v1",
                  "aggregateType": "acolhimento",
                  "aggregateId": "8206e539-fc43-42f8-8bd5-bf6164b835dc",
                  "eventVersion": 1,
                  "occurredAt": "2026-03-02T15:10:00",
                  "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
                  "userAgent": "Mozilla/5.0",
                  "originIp": "187.11.22.33",
                  "status": "REJECTED",
                  "reasonCode": "VALIDATION_ERROR",
                  "errorMessage": "Falha de validacao do payload/evento.",
                  "rejectedAt": "2026-03-02T15:10:00"
                }
                """;
    }
}
