package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.ProcessedEventPort;

@Component
public class RetentionWorker {

    private static final int FIXED_DELAY_MS = 3_600_000;
    private static final int OUTBOX_RETENTION_HOURS = 48;
    private static final int PROCESSED_RETENTION_DAYS = 90;

    private final OutboxEventPort outboxEventPort;
    private final ProcessedEventPort processedEventPort;

    public RetentionWorker(OutboxEventPort outboxEventPort,
            ProcessedEventPort processedEventPort) {
        this.outboxEventPort = outboxEventPort;
        this.processedEventPort = processedEventPort;
    }

    @Scheduled(fixedDelay = FIXED_DELAY_MS)
    public void purge() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime outboxCutoff = now.minusHours(OUTBOX_RETENTION_HOURS);
        LocalDateTime processedCutoff = now.minusDays(PROCESSED_RETENTION_DAYS);

        outboxEventPort.deleteByStatusAndCreatedAtBefore(OutboxStatus.PUBLISHED, outboxCutoff);
        outboxEventPort.deleteByStatusAndCreatedAtBefore(OutboxStatus.DEAD, outboxCutoff);
        processedEventPort.deleteByProcessedAtBefore(processedCutoff);
    }
}
