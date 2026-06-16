package com.humanizar.acolhimento.infrastructure.messaging.outbound.recall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@ExtendWith(MockitoExtension.class)
class PendingRecallTest {

    private static final String TARGET = "humanizar-nucleo-relacionamento";

    @Mock
    private PendingTargetStatusPort pendingTargetStatusPort;

    @Mock
    private PendingAcolhimentoPort pendingAcolhimentoPort;

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private RecallPolicy recallPolicy;

    @InjectMocks
    private PendingRecall pendingRecall;

    @Test
    void shouldEarlyReturnWhenNoStaleTargets() {
        when(pendingTargetStatusPort.findStaleTargets(eq(Status.PENDING), any(), anyInt()))
                .thenReturn(List.of());

        pendingRecall.recall(50);

        verify(pendingAcolhimentoPort, never()).findByEventId(any());
        verify(outboxEventPort, never()).save(any());
    }

    @Test
    void shouldReDriveStalePendingTargetResettingDeadOutbox() {
        UUID eventId = UUID.randomUUID();
        PendingTargetStatus target = target(eventId, 0);
        OutboxEvent outbox = OutboxEvent.builder()
                .eventId(eventId)
                .status(OutboxStatus.DEAD)
                .attemptCount(5)
                .maxAttempts(5)
                .lockedBy(UUID.randomUUID())
                .lastError("Broker NACK")
                .build();

        when(pendingTargetStatusPort.findStaleTargets(eq(Status.PENDING), any(), anyInt())).thenReturn(List.of(target));
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(pending(eventId, Status.PENDING)));
        when(recallPolicy.isExhausted(0)).thenReturn(false);
        when(outboxEventPort.findByEventIdForUpdate(eventId)).thenReturn(Optional.of(outbox));
        when(recallPolicy.nextRecallAt(1)).thenReturn(LocalDateTime.now().plusMinutes(1));

        pendingRecall.recall(50);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventPort).save(outboxCaptor.capture());
        OutboxEvent reDriven = outboxCaptor.getValue();
        assertEquals(OutboxStatus.NEW, reDriven.getStatus());
        assertEquals(0, reDriven.getAttemptCount());
        assertNull(reDriven.getLockedBy());
        assertNull(reDriven.getLastError());

        ArgumentCaptor<PendingTargetStatus> targetCaptor = ArgumentCaptor.forClass(PendingTargetStatus.class);
        verify(pendingTargetStatusPort).save(targetCaptor.capture());
        assertEquals(1, targetCaptor.getValue().getRecallAttempts());
    }

    @Test
    void shouldSkipWhenRelayHoldsOutboxLock() {
        UUID eventId = UUID.randomUUID();
        PendingTargetStatus target = target(eventId, 0);

        when(pendingTargetStatusPort.findStaleTargets(eq(Status.PENDING), any(), anyInt())).thenReturn(List.of(target));
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(pending(eventId, Status.PENDING)));
        when(recallPolicy.isExhausted(0)).thenReturn(false);
        when(outboxEventPort.findByEventIdForUpdate(eventId)).thenReturn(Optional.empty());
        when(outboxEventPort.findByEventId(eventId)).thenReturn(Optional.of(OutboxEvent.builder().eventId(eventId).build()));

        pendingRecall.recall(50);

        verify(outboxEventPort, never()).save(any());
        verify(pendingTargetStatusPort, never()).save(any());
    }

    @Test
    void shouldEscalateToErrorWhenNoOutbox() {
        UUID eventId = UUID.randomUUID();
        PendingTargetStatus target = target(eventId, 0);
        PendingAcolhimento parent = pending(eventId, Status.PENDING);

        when(pendingTargetStatusPort.findStaleTargets(eq(Status.PENDING), any(), anyInt())).thenReturn(List.of(target));
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(parent));
        when(recallPolicy.isExhausted(0)).thenReturn(false);
        when(outboxEventPort.findByEventIdForUpdate(eventId)).thenReturn(Optional.empty());
        when(outboxEventPort.findByEventId(eventId)).thenReturn(Optional.empty());

        pendingRecall.recall(50);

        ArgumentCaptor<PendingTargetStatus> targetCaptor = ArgumentCaptor.forClass(PendingTargetStatus.class);
        verify(pendingTargetStatusPort).save(targetCaptor.capture());
        assertEquals(Status.ERROR, targetCaptor.getValue().getStatus());
        assertEquals(Status.ERROR, parent.getStatus());
        verify(pendingAcolhimentoPort).save(parent);
        verify(outboxEventPort, never()).save(any());
    }

    @Test
    void shouldEscalateToErrorWhenCapReached() {
        UUID eventId = UUID.randomUUID();
        PendingTargetStatus target = target(eventId, 10);
        PendingAcolhimento parent = pending(eventId, Status.PENDING);

        when(pendingTargetStatusPort.findStaleTargets(eq(Status.PENDING), any(), anyInt())).thenReturn(List.of(target));
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(parent));
        when(recallPolicy.isExhausted(10)).thenReturn(true);

        pendingRecall.recall(50);

        assertEquals(Status.ERROR, target.getStatus());
        assertEquals(Status.ERROR, parent.getStatus());
        verify(pendingTargetStatusPort).save(target);
        verify(pendingAcolhimentoPort).save(parent);
        verify(outboxEventPort, never()).findByEventIdForUpdate(any());
    }

    @Test
    void shouldSkipWhenParentMissing() {
        UUID eventId = UUID.randomUUID();
        when(pendingTargetStatusPort.findStaleTargets(eq(Status.PENDING), any(), anyInt()))
                .thenReturn(List.of(target(eventId, 0)));
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.empty());

        pendingRecall.recall(50);

        verify(outboxEventPort, never()).findByEventIdForUpdate(any());
        verify(pendingTargetStatusPort, never()).save(any());
    }

    @Test
    void shouldSkipWhenParentNotPending() {
        UUID eventId = UUID.randomUUID();
        when(pendingTargetStatusPort.findStaleTargets(eq(Status.PENDING), any(), anyInt()))
                .thenReturn(List.of(target(eventId, 0)));
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(pending(eventId, Status.SUCCESS)));

        pendingRecall.recall(50);

        verify(outboxEventPort, never()).findByEventIdForUpdate(any());
        verify(pendingTargetStatusPort, never()).save(any());
    }

    private PendingTargetStatus target(UUID eventId, int attempts) {
        return PendingTargetStatus.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .targetService(TARGET)
                .status(Status.PENDING)
                .recallAttempts(attempts)
                .nextRecallAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    private PendingAcolhimento pending(UUID eventId, Status status) {
        return PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(UUID.randomUUID())
                .status(status)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
    }
}
