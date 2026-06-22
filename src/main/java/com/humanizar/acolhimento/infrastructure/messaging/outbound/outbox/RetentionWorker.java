package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.ProcessedEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@Component
public class RetentionWorker {

    private static final int FIXED_DELAY_MS = 3_600_000;
    private static final int OUTBOX_RETENTION_HOURS = 2_160;
    private static final int PROCESSED_RETENTION_DAYS = 90;
    private static final int PENDING_RETENTION_DAYS = 90;
    private static final List<Status> PENDING_TERMINAL_STATUSES = List.of(Status.SUCCESS, Status.ERROR);

    private final OutboxEventPort outboxEventPort;
    private final ProcessedEventPort processedEventPort;
    private final PendingAcolhimentoPort pendingAcolhimentoPort;
    private final PendingTargetStatusPort pendingTargetStatusPort;

    public RetentionWorker(OutboxEventPort outboxEventPort,
            ProcessedEventPort processedEventPort,
            PendingAcolhimentoPort pendingAcolhimentoPort,
            PendingTargetStatusPort pendingTargetStatusPort) {
        this.outboxEventPort = outboxEventPort;
        this.processedEventPort = processedEventPort;
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
        this.pendingTargetStatusPort = pendingTargetStatusPort;
    }

    @Scheduled(fixedDelay = FIXED_DELAY_MS)
    public void purge() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime outboxCutoff = now.minusHours(OUTBOX_RETENTION_HOURS);
        LocalDateTime processedCutoff = now.minusDays(PROCESSED_RETENTION_DAYS);
        LocalDateTime pendingCutoff = now.minusDays(PENDING_RETENTION_DAYS);

        outboxEventPort.deleteByStatusAndCreatedAtBefore(OutboxStatus.PUBLISHED, outboxCutoff);
        outboxEventPort.deleteByStatusAndCreatedAtBefore(OutboxStatus.DEAD, outboxCutoff);
        processedEventPort.deleteByProcessedAtBefore(processedCutoff);
        pendingAcolhimentoPort.deleteByStatusInAndCreatedAtBefore(PENDING_TERMINAL_STATUSES, pendingCutoff);
        pendingTargetStatusPort.deleteByStatusInAndCreatedAtBefore(PENDING_TERMINAL_STATUSES, pendingCutoff);
    }
}
