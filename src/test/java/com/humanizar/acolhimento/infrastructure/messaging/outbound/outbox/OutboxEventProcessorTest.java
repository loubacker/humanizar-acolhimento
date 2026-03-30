package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.application.usecase.central.FindPendingByEventIdUseCase;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;
import com.humanizar.acolhimento.infrastructure.messaging.outbound.rabbit.RabbitOutboxPublisher;

@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private PendingTargetStatusPort pendingTargetStatusPort;

    @Mock
    private RabbitOutboxPublisher rabbitOutboxPublisher;

    @Mock
    private OutboxRetryPolicy outboxRetryPolicy;

    @Mock
    private FindPendingByEventIdUseCase findPendingByEventIdUseCase;

    @InjectMocks
    private OutboxEventProcessor outboxEventProcessor;

    @Captor
    private ArgumentCaptor<PendingTargetStatus> pendingTargetCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(findPendingByEventIdUseCase.execute(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    void shouldClaimAndLockEventsFromOutbox() {
        OutboxEvent event = newOutboxEvent(OutboxStatus.NEW, RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        when(outboxEventPort.findPendingForRelay(anyList(), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(event));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<OutboxEvent> claimed = outboxEventProcessor.claimBatch(50);

        assertEquals(1, claimed.size());
        assertEquals(OutboxStatus.LOCKED, event.getStatus());
        assertNotNull(event.getLockedBy());
        assertNotNull(event.getNextRetryAt());
        verify(outboxEventPort).save(event);
    }

    @Test
    void shouldPublishAndCreatePendingTargetOnSuccess() {
        OutboxEvent event = claimOneEvent(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(findPendingByEventIdUseCase.execute(event.getEventId())).thenReturn(Optional.of(newPending(event)));
        when(pendingTargetStatusPort.findByEventId(event.getEventId())).thenReturn(List.of());
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxEventProcessor.processEvent(event);

        InOrder inOrder = inOrder(pendingTargetStatusPort, rabbitOutboxPublisher);
        inOrder.verify(pendingTargetStatusPort, times(2)).save(any(PendingTargetStatus.class));
        inOrder.verify(rabbitOutboxPublisher).publish(event);

        verify(pendingTargetStatusPort, times(2)).save(pendingTargetCaptor.capture());
        List<PendingTargetStatus> savedTargets = pendingTargetCaptor.getAllValues();
        assertEquals(2, savedTargets.size());
        assertEquals(TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO, savedTargets.getFirst().getTargetService());
        assertEquals(Status.PENDING, savedTargets.getFirst().getStatus());
        assertEquals(TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO, savedTargets.get(1).getTargetService());
        assertEquals(Status.PENDING, savedTargets.get(1).getStatus());

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(null, event.getLastError());
        assertEquals(null, event.getLockedBy());
    }

    @Test
    void shouldCreateProgramTargetAsOnHoldForDeleteV2Command() {
        OutboxEvent event = claimOneEvent(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V2);
        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(findPendingByEventIdUseCase.execute(event.getEventId())).thenReturn(Optional.of(newPending(event)));
        when(pendingTargetStatusPort.findByEventId(event.getEventId())).thenReturn(List.of());
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxEventProcessor.processEvent(event);

        verify(pendingTargetStatusPort, times(2)).save(pendingTargetCaptor.capture());
        List<PendingTargetStatus> savedTargets = pendingTargetCaptor.getAllValues();
        assertEquals(Status.PENDING, savedTargets.getFirst().getStatus());
        assertEquals(Status.ON_HOLD, savedTargets.get(1).getStatus());
        assertEquals(TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO, savedTargets.get(1).getTargetService());
        verify(rabbitOutboxPublisher).publish(event);
    }

    @Test
    void shouldProcessUsingFreshEntityInsteadOfStaleInput() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent staleEvent = newOutboxEventWithEventId(eventId, OutboxStatus.NEW, RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        OutboxEvent freshEvent = newOutboxEventWithEventId(eventId, OutboxStatus.NEW, RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);

        when(outboxEventPort.findPendingForRelay(anyList(), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(freshEvent));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxEventProcessor.claimBatch(1);

        when(outboxEventPort.findByEventId(eventId)).thenReturn(Optional.of(freshEvent));
        when(findPendingByEventIdUseCase.execute(eventId)).thenReturn(Optional.of(newPending(freshEvent)));
        when(pendingTargetStatusPort.findByEventId(eventId)).thenReturn(List.of());

        outboxEventProcessor.processEvent(staleEvent);

        verify(rabbitOutboxPublisher).publish(freshEvent);
        verify(outboxEventPort, never()).save(staleEvent);
        assertEquals(OutboxStatus.NEW, staleEvent.getStatus());
        assertEquals(OutboxStatus.PUBLISHED, freshEvent.getStatus());
    }

    @Test
    void shouldNotCreatePendingTargetsWhenAllAlreadyExist() {
        OutboxEvent event = claimOneEvent(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(findPendingByEventIdUseCase.execute(event.getEventId())).thenReturn(Optional.of(newPending(event)));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pendingTargetStatusPort.findByEventId(event.getEventId())).thenReturn(List.of(
                new PendingTargetStatus(UUID.randomUUID(), event.getEventId(), TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO, Status.PENDING),
                new PendingTargetStatus(UUID.randomUUID(), event.getEventId(), TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO, Status.PENDING)));

        outboxEventProcessor.processEvent(event);

        verify(rabbitOutboxPublisher).publish(event);
        verify(pendingTargetStatusPort, never()).save(any(PendingTargetStatus.class));
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void shouldCreateMissingPendingTargetWhenOnlyOneExists() {
        OutboxEvent event = claimOneEvent(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(findPendingByEventIdUseCase.execute(event.getEventId())).thenReturn(Optional.of(newPending(event)));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pendingTargetStatusPort.findByEventId(event.getEventId())).thenReturn(List.of(
                new PendingTargetStatus(UUID.randomUUID(), event.getEventId(), TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO, Status.PENDING)));

        outboxEventProcessor.processEvent(event);

        verify(rabbitOutboxPublisher).publish(event);
        verify(pendingTargetStatusPort).save(pendingTargetCaptor.capture());
        PendingTargetStatus savedTarget = pendingTargetCaptor.getValue();
        assertEquals(TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO, savedTarget.getTargetService());
        assertEquals(Status.PENDING, savedTarget.getStatus());
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void shouldMarkAsFailedWhenPublishThrowsAndRetryNotExhausted() {
        OutboxEvent event = claimOneEvent(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        event.setAttemptCount(0);
        event.setMaxAttempts(3);

        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(outboxRetryPolicy.isExhausted(1, 3)).thenReturn(false);
        when(outboxRetryPolicy.nextRetryAt(1)).thenReturn(LocalDateTime.now().plusSeconds(10));
        doThrow(new RuntimeException("rabbit down")).when(rabbitOutboxPublisher).publish(event);

        outboxEventProcessor.processEvent(event);

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertNotNull(event.getNextRetryAt());
        assertNotNull(event.getLastError());
        assertEquals(null, event.getLockedBy());
        verify(pendingTargetStatusPort, never()).findByEventId(any(UUID.class));
    }

    @Test
    void shouldMarkAsDeadWhenRetryIsExhausted() {
        OutboxEvent event = claimOneEvent(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        event.setAttemptCount(0);
        event.setMaxAttempts(1);

        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(outboxRetryPolicy.isExhausted(1, 1)).thenReturn(true);
        doThrow(new RuntimeException("erro final")).when(rabbitOutboxPublisher).publish(event);

        outboxEventProcessor.processEvent(event);

        assertEquals(OutboxStatus.DEAD, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertNotNull(event.getLastError());
        assertEquals(null, event.getLockedBy());
    }

    private OutboxEvent claimOneEvent(String routingKey) {
        OutboxEvent event = newOutboxEvent(OutboxStatus.NEW, routingKey);
        when(outboxEventPort.findPendingForRelay(anyList(), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(event));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        outboxEventProcessor.claimBatch(1);
        return event;
    }

    private PendingAcolhimento newPending(OutboxEvent event) {
        return PendingAcolhimento.builder()
                .eventId(event.getEventId())
                .correlationId(event.getCorrelationId())
                .patientId(UUID.randomUUID())
                .operationType(OperationType.DELETE)
                .status(Status.PENDING)
                .payloadSnapshot("{}")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private OutboxEvent newOutboxEvent(OutboxStatus status, String routingKey) {
        return newOutboxEventWithEventId(UUID.randomUUID(), status, routingKey);
    }

    private OutboxEvent newOutboxEventWithEventId(UUID eventId, OutboxStatus status, String routingKey) {
        return OutboxEvent.builder()
                .id(1L)
                .eventId(eventId)
                .correlationId(UUID.randomUUID())
                .exchangeName("humanizar.acolhimento.command")
                .routingKey(routingKey)
                .aggregateType("acolhimento")
                .aggregateId(UUID.randomUUID())
                .payload("{\"ok\":true}")
                .status(status)
                .attemptCount(0)
                .maxAttempts(3)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
