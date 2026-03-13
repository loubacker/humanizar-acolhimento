package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;

@ExtendWith(MockitoExtension.class)
class OutboxRelayWorkerTest {

    @Mock
    private OutboxEventProcessor outboxEventProcessor;

    @Mock
    private Executor outboxExecutor;

    @InjectMocks
    private OutboxRelayWorker outboxRelayWorker;

    @Test
    void shouldProcessClaimedEvents() {
        OutboxEvent event1 = newOutboxEvent();
        OutboxEvent event2 = newOutboxEvent();
        when(outboxEventProcessor.claimBatch(50)).thenReturn(List.of(event1, event2));
        runExecutorInline();

        outboxRelayWorker.relay();

        verify(outboxEventProcessor).claimBatch(50);
        verify(outboxEventProcessor, times(1)).processEvent(event1);
        verify(outboxEventProcessor, times(1)).processEvent(event2);
    }

    @Test
    void shouldSkipProcessingWhenBatchIsEmpty() {
        when(outboxEventProcessor.claimBatch(50)).thenReturn(List.of());

        outboxRelayWorker.relay();

        verify(outboxEventProcessor).claimBatch(50);
        verify(outboxEventProcessor, never()).processEvent(org.mockito.ArgumentMatchers.any(OutboxEvent.class));
    }

    private void runExecutorInline() {
        org.mockito.Mockito.doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(outboxExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
    }

    private OutboxEvent newOutboxEvent() {
        return OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .routingKey("cmd.acolhimento.created.v1")
                .exchangeName("humanizar.acolhimento.command")
                .status(OutboxStatus.LOCKED)
                .build();
    }
}
