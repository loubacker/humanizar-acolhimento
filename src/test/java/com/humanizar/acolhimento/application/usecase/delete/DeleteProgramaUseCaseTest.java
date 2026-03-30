package com.humanizar.acolhimento.application.usecase.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.application.inbound.mapper.InboundEnvelopeMapper;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.mapper.OutboundDeleteMapper;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveAcolhimentoUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@ExtendWith(MockitoExtension.class)
class DeleteProgramaUseCaseTest {

    private static final String PRODUCER_SERVICE = "humanizar-acolhimento";
    private static final String USER_AGENT = "JUnit";
    private static final String ORIGIN_IP = "127.0.0.1";

    @Mock
    private OutboxEventPort outboxEventPort;
    @Mock
    private PendingTargetStatusPort pendingTargetStatusPort;
    @Mock
    private RetrieveAcolhimentoUseCase retrieveAcolhimentoUseCase;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private DeleteProgramaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteProgramaUseCase(
                outboxEventPort,
                pendingTargetStatusPort,
                retrieveAcolhimentoUseCase,
                new InboundEnvelopeMapper(),
                new OutboundDeleteMapper(),
                objectMapper);
    }

    @Test
    void shouldCreateDeletedV1OutboxAndPromoteProgramaTargetToPending() {
        UUID eventId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        PendingAcolhimento pending = PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .patientId(patientId)
                .operationType(OperationType.DELETE)
                .status(Status.PENDING)
                .payloadSnapshot("{}")
                .createdAt(LocalDateTime.now())
                .build();

        Acolhimento acolhimento = Acolhimento.builder()
                .id(aggregateId)
                .patientId(patientId)
                .build();

        PendingTargetStatus programaOnHold = new PendingTargetStatus(
                UUID.randomUUID(),
                eventId,
                TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO,
                Status.ON_HOLD);

        when(retrieveAcolhimentoUseCase.execute(patientId, correlationId.toString()))
                .thenReturn(acolhimento);
        when(outboxEventPort.findByEventId(eventId))
                .thenReturn(Optional.of(sourceDeletedV2Event(eventId, correlationId, patientId, actorId)));
        when(pendingTargetStatusPort.findByEventIdAndTargetService(eventId, TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO))
                .thenReturn(Optional.of(programaOnHold));
        when(outboxEventPort.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(pending);

        verify(outboxEventPort).save(outboxEventCaptor.capture());
        OutboxEvent saved = outboxEventCaptor.getValue();
        assertEquals(correlationId, saved.getCorrelationId());
        assertEquals(aggregateId, saved.getAggregateId());
        assertEquals(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1, saved.getRoutingKey());
        assertEquals(OutboxStatus.NEW, saved.getStatus());
        assertNotNull(saved.getPayload());
        assertNotNull(saved.getEventId());
        assertEquals(actorId, saved.getActorId());
        assertEquals(USER_AGENT, saved.getUserAgent());
        assertEquals(ORIGIN_IP, saved.getOriginIp());

        assertEquals(Status.PENDING, programaOnHold.getStatus());
        verify(pendingTargetStatusPort).save(programaOnHold);

        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> savedEnvelope = readEnvelope(saved.getPayload());
        assertEquals(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1, savedEnvelope.routingKey());
        assertEquals(actorId, savedEnvelope.actorId());
        assertEquals(USER_AGENT, savedEnvelope.userAgent());
        assertEquals(ORIGIN_IP, savedEnvelope.originIp());
        assertEquals(patientId, savedEnvelope.payload().patientId());
    }

    @Test
    void shouldFailWhenSourceDeletedV2OutboxIsMissing() {
        UUID eventId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        PendingAcolhimento pending = PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .patientId(patientId)
                .operationType(OperationType.DELETE)
                .status(Status.PENDING)
                .payloadSnapshot("{}")
                .build();

        when(outboxEventPort.findByEventId(eventId)).thenReturn(Optional.empty());

        AcolhimentoException ex = assertThrows(AcolhimentoException.class, () -> useCase.execute(pending));
        assertEquals(ReasonCode.PERSISTENCE_FAILURE, ex.getReasonCode());
        verify(retrieveAcolhimentoUseCase, never()).execute(any(UUID.class), any(String.class));
        verify(outboxEventPort, never()).save(any(OutboxEvent.class));
        verify(pendingTargetStatusPort, never()).save(any(PendingTargetStatus.class));
    }

    @Test
    void shouldFailWhenSourceDeletedV2EnvelopeHasMissingMetadata() {
        UUID eventId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        PendingAcolhimento pending = PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .patientId(patientId)
                .operationType(OperationType.DELETE)
                .status(Status.PENDING)
                .payloadSnapshot("{}")
                .build();

        when(outboxEventPort.findByEventId(eventId))
                .thenReturn(Optional.of(sourceDeletedV2Event(eventId, correlationId, patientId, null)));

        AcolhimentoException ex = assertThrows(AcolhimentoException.class, () -> useCase.execute(pending));
        assertEquals(ReasonCode.PERSISTENCE_FAILURE, ex.getReasonCode());
        verify(outboxEventPort, never()).save(any(OutboxEvent.class));
        verify(pendingTargetStatusPort, never()).save(any(PendingTargetStatus.class));
    }

    private OutboxEvent sourceDeletedV2Event(
            UUID eventId,
            UUID correlationId,
            UUID patientId,
            UUID actorId) {
        OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> sourceEnvelope = new OutboundEnvelopeDTO<>(
                eventId,
                correlationId,
                PRODUCER_SERVICE,
                "humanizar.acolhimento.command",
                RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2,
                "acolhimento",
                UUID.randomUUID(),
                1,
                LocalDateTime.now(),
                actorId,
                USER_AGENT,
                ORIGIN_IP,
                new AcolhimentoCommandDeletedDTO(patientId));

        return OutboxEvent.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .producerService(PRODUCER_SERVICE)
                .exchangeName("humanizar.acolhimento.command")
                .routingKey(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2)
                .aggregateType("acolhimento")
                .aggregateId(UUID.randomUUID())
                .eventVersion((short) 1)
                .payload(writeEnvelope(sourceEnvelope))
                .status(OutboxStatus.PUBLISHED)
                .build();
    }

    private String writeEnvelope(OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> readEnvelope(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
