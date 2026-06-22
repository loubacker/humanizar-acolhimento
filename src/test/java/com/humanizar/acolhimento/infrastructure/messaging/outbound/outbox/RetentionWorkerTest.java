package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.ProcessedEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@ExtendWith(MockitoExtension.class)
class RetentionWorkerTest {

    @Mock
    private OutboxEventPort outboxEventPort;

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private PendingAcolhimentoPort pendingAcolhimentoPort;

    @Mock
    private PendingTargetStatusPort pendingTargetStatusPort;

    @InjectMocks
    private RetentionWorker retentionWorker;

    @Test
    void shouldUseNinetyDayRetentionForOutboxEvents() {
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(90).minusSeconds(1);

        retentionWorker.purge();

        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(90).plusSeconds(1);

        ArgumentCaptor<LocalDateTime> outboxCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxEventPort).deleteByStatusAndCreatedAtBefore(
                eq(OutboxStatus.PUBLISHED), outboxCutoffCaptor.capture());
        verify(outboxEventPort).deleteByStatusAndCreatedAtBefore(
                eq(OutboxStatus.DEAD), outboxCutoffCaptor.capture());

        for (LocalDateTime cutoff : outboxCutoffCaptor.getAllValues()) {
            assertTrue(cutoff.isAfter(earliestExpectedCutoff));
            assertTrue(cutoff.isBefore(latestExpectedCutoff));
        }
    }

    @Test
    void shouldKeepNinetyDayRetentionForProcessedEvents() {
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(90).minusSeconds(1);

        retentionWorker.purge();

        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(90).plusSeconds(1);

        ArgumentCaptor<LocalDateTime> processedCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(processedEventPort).deleteByProcessedAtBefore(processedCutoffCaptor.capture());

        LocalDateTime cutoff = processedCutoffCaptor.getValue();
        assertTrue(cutoff.isAfter(earliestExpectedCutoff));
        assertTrue(cutoff.isBefore(latestExpectedCutoff));
    }

    @Test
    void shouldPurgeOnlyTerminalPendingAcolhimentoAtNinetyDays() {
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(90).minusSeconds(1);

        retentionWorker.purge();

        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(90).plusSeconds(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Status>> statusCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pendingAcolhimentoPort).deleteByStatusInAndCreatedAtBefore(
                statusCaptor.capture(), cutoffCaptor.capture());

        assertEquals(List.of(Status.SUCCESS, Status.ERROR), statusCaptor.getValue());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertTrue(cutoff.isAfter(earliestExpectedCutoff));
        assertTrue(cutoff.isBefore(latestExpectedCutoff));
    }

    @Test
    void shouldPurgeOnlyTerminalPendingTargetStatusAtNinetyDays() {
        LocalDateTime earliestExpectedCutoff = LocalDateTime.now().minusDays(90).minusSeconds(1);

        retentionWorker.purge();

        LocalDateTime latestExpectedCutoff = LocalDateTime.now().minusDays(90).plusSeconds(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Status>> statusCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pendingTargetStatusPort).deleteByStatusInAndCreatedAtBefore(
                statusCaptor.capture(), cutoffCaptor.capture());

        assertEquals(List.of(Status.SUCCESS, Status.ERROR), statusCaptor.getValue());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertTrue(cutoff.isAfter(earliestExpectedCutoff));
        assertTrue(cutoff.isBefore(latestExpectedCutoff));
    }
}
