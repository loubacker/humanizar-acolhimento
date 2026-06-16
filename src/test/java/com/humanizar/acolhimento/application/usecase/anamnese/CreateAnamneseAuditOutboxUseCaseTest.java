package com.humanizar.acolhimento.application.usecase.anamnese;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@ExtendWith(MockitoExtension.class)
class CreateAnamneseAuditOutboxUseCaseTest {

    @Mock
    private OutboxEventPort outboxEventPort;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private CreateAnamneseAuditOutboxUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAnamneseAuditOutboxUseCase(
                outboxEventPort,
                new JsonPayloadSerializer(new ObjectMapper().registerModule(new JavaTimeModule())));
    }

    @Test
    void shouldPersistAuditOutboxEventWithStatusAuditAndEnvelopeTracing() {
        UUID correlationId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        AnamneseDTO state = new AnamneseDTO(LocalDate.of(2026, 3, 2), LocalTime.of(11, 35), "obs");

        HttpEnvelopeDTO<AnamneseDTO> envelope = new HttpEnvelopeDTO<>(
                correlationId,
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                actorId,
                "JUnit",
                "187.11.22.33",
                state);

        useCase.execute(
                envelope,
                eventId,
                patientId,
                RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_CREATED_V1,
                state);

        verify(outboxEventPort).save(outboxEventCaptor.capture());
        OutboxEvent saved = outboxEventCaptor.getValue();

        assertEquals(OutboxStatus.AUDIT, saved.getStatus());
        assertEquals(eventId, saved.getEventId());
        assertEquals(correlationId, saved.getCorrelationId());
        assertEquals(actorId, saved.getActorId());
        assertEquals("JUnit", saved.getUserAgent());
        assertEquals("187.11.22.33", saved.getOriginIp());
        assertEquals(patientId, saved.getAggregateId());
        assertEquals(ExchangeCatalog.ACOLHIMENTO_EVENT, saved.getExchangeName());
        assertEquals(RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_CREATED_V1, saved.getRoutingKey());
        assertTrue(saved.getPayload().contains(actorId.toString()));
        assertTrue(saved.getPayload().contains("obs"));
    }

    @Test
    void shouldPersistAuditOutboxEventWithNullStateForDelete() {
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        HttpEnvelopeDTO<AnamneseDTO> envelope = new HttpEnvelopeDTO<>(
                correlationId,
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "187.11.22.33",
                null);

        useCase.execute(
                envelope,
                eventId,
                patientId,
                RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_DELETED_V1,
                null);

        verify(outboxEventPort).save(outboxEventCaptor.capture());
        OutboxEvent saved = outboxEventCaptor.getValue();

        assertEquals(OutboxStatus.AUDIT, saved.getStatus());
        assertEquals(RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_DELETED_V1, saved.getRoutingKey());
    }
}
