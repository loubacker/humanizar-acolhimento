package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;
import com.humanizar.acolhimento.infrastructure.messaging.outbound.rabbit.RabbitOutboxPublisher;

@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

    private static final String TARGET_NUCLEO_RELACIONAMENTO = "humanizar-nucleo-relacionamento";

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private PendingTargetStatusPort pendingTargetStatusPort;

    @Mock
    private RabbitOutboxPublisher rabbitOutboxPublisher;

    @Mock
    private OutboxRetryPolicy outboxRetryPolicy;

    @InjectMocks
    private OutboxEventProcessor outboxEventProcessor;

    @Captor
    private ArgumentCaptor<List<PendingTargetStatus>> pendingTargetListCaptor;

    @Test
    void shouldClaimAndLockEventsFromOutbox() {
        OutboxEvent event = newOutboxEvent(OutboxStatus.NEW);
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
        OutboxEvent event = claimOneEvent();
        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(pendingTargetStatusPort.findByEventId(event.getEventId())).thenReturn(List.of());
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxEventProcessor.processEvent(event);

        verify(rabbitOutboxPublisher).publish(event);
        verify(pendingTargetStatusPort).saveAll(pendingTargetListCaptor.capture());
        List<PendingTargetStatus> savedTargets = pendingTargetListCaptor.getValue();
        assertEquals(1, savedTargets.size());
        assertEquals(TARGET_NUCLEO_RELACIONAMENTO, savedTargets.getFirst().getTargetService());
        assertEquals(Status.PENDING, savedTargets.getFirst().getStatus());

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(null, event.getLastError());
        assertEquals(null, event.getLockedBy());
    }

    @Test
    void shouldProcessUsingFreshEntityInsteadOfStaleInput() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent staleEvent = newOutboxEventWithEventId(eventId, OutboxStatus.NEW);
        OutboxEvent freshEvent = newOutboxEventWithEventId(eventId, OutboxStatus.NEW);

        when(outboxEventPort.findPendingForRelay(anyList(), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(freshEvent));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxEventProcessor.claimBatch(1);

        when(outboxEventPort.findByEventId(eventId)).thenReturn(Optional.of(freshEvent));
        when(pendingTargetStatusPort.findByEventId(eventId)).thenReturn(List.of());

        outboxEventProcessor.processEvent(staleEvent);

        verify(rabbitOutboxPublisher).publish(freshEvent);
        verify(outboxEventPort, never()).save(staleEvent);
        assertEquals(OutboxStatus.NEW, staleEvent.getStatus());
        assertEquals(OutboxStatus.PUBLISHED, freshEvent.getStatus());
    }

    @Test
    void shouldNotCreatePendingTargetWhenAlreadyExists() {
        OutboxEvent event = claimOneEvent();
        when(outboxEventPort.findByEventId(event.getEventId())).thenReturn(Optional.of(event));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pendingTargetStatusPort.findByEventId(event.getEventId())).thenReturn(List.of(
                new PendingTargetStatus(UUID.randomUUID(), event.getEventId(), TARGET_NUCLEO_RELACIONAMENTO, Status.PENDING)));

        outboxEventProcessor.processEvent(event);

        verify(rabbitOutboxPublisher).publish(event);
        verify(pendingTargetStatusPort, never()).saveAll(anyList());
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void shouldMarkAsFailedWhenPublishThrowsAndRetryNotExhausted() {
        OutboxEvent event = claimOneEvent();
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
        OutboxEvent event = claimOneEvent();
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

    private OutboxEvent claimOneEvent() {
        OutboxEvent event = newOutboxEvent(OutboxStatus.NEW);
        when(outboxEventPort.findPendingForRelay(anyList(), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(event));
        when(outboxEventPort.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        outboxEventProcessor.claimBatch(1);
        return event;
    }

    private OutboxEvent newOutboxEvent(OutboxStatus status) {
        return newOutboxEventWithEventId(UUID.randomUUID(), status);
    }

    private OutboxEvent newOutboxEventWithEventId(UUID eventId, OutboxStatus status) {
        return OutboxEvent.builder()
                .id(1L)
                .eventId(eventId)
                .correlationId(UUID.randomUUID())
                .exchangeName("humanizar.acolhimento.command")
                .routingKey("cmd.acolhimento.created.v1")
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
