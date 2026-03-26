package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;
import com.humanizar.acolhimento.infrastructure.messaging.outbound.rabbit.RabbitOutboxPublisher;

@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    private final UUID instanceId = UUID.randomUUID();
    private final OutboxEventPort outboxEventPort;
    private final PendingTargetStatusPort pendingTargetStatusPort;
    private final RabbitOutboxPublisher rabbitOutboxPublisher;
    private final OutboxRetryPolicy retryPolicy;

    public OutboxEventProcessor(
            OutboxEventPort outboxEventPort,
            PendingTargetStatusPort pendingTargetStatusPort,
            RabbitOutboxPublisher rabbitOutboxPublisher,
            OutboxRetryPolicy retryPolicy) {
        this.outboxEventPort = outboxEventPort;
        this.pendingTargetStatusPort = pendingTargetStatusPort;
        this.rabbitOutboxPublisher = rabbitOutboxPublisher;
        this.retryPolicy = retryPolicy;
        log.info("OutboxEventProcessor iniciado. instanceId={}", instanceId);
    }

    @Transactional
    public List<OutboxEvent> claimBatch(int batchSize) {
        List<OutboxEvent> pending = outboxEventPort.findPendingForRelay(
                List.of(OutboxStatus.NEW, OutboxStatus.FAILED, OutboxStatus.LOCKED),
                LocalDateTime.now(),
                batchSize);

        LocalDateTime lockExpiry = LocalDateTime.now().plus(LOCK_TIMEOUT);
        for (OutboxEvent event : pending) {
            event.setStatus(OutboxStatus.LOCKED);
            event.setNextRetryAt(lockExpiry);
            event.setLockedBy(instanceId);
            outboxEventPort.save(event);
        }

        return pending;
    }

    @Transactional
    public void processEvent(OutboxEvent event) {
        OutboxEvent processingEvent = outboxEventPort.findByEventId(event.getEventId()).orElse(null);
        if (processingEvent == null
                || processingEvent.getStatus() != OutboxStatus.LOCKED
                || !instanceId.equals(processingEvent.getLockedBy())) {
            log.warn("Fencing check falhou. eventId={}, expectedOwner={}, actualOwner={}",
                    event.getEventId(),
                    instanceId,
                    processingEvent != null ? processingEvent.getLockedBy() : "N/A");
            return;
        }

        try {
            rabbitOutboxPublisher.publish(processingEvent);
            createMissingPendingTargets(processingEvent.getEventId());

            processingEvent.setStatus(OutboxStatus.PUBLISHED);
            processingEvent.setPublishedAt(LocalDateTime.now());
            processingEvent.setLastError(null);
            processingEvent.setLockedBy(null);
            outboxEventPort.save(processingEvent);

        } catch (Exception ex) {
            int newAttemptCount = Objects.requireNonNullElse(processingEvent.getAttemptCount(), 0) + 1;
            int maxAttempts = Objects.requireNonNullElse(
                    processingEvent.getMaxAttempts(), retryPolicy.getDefaultMaxAttempts());

            processingEvent.setAttemptCount(newAttemptCount);
            processingEvent.setLastError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            processingEvent.setLockedBy(null);

            if (retryPolicy.isExhausted(newAttemptCount, maxAttempts)) {
                processingEvent.setStatus(OutboxStatus.DEAD);
                log.error("Evento movido para DEAD apos {} tentativas. eventId={}",
                        newAttemptCount, processingEvent.getEventId(), ex);
            } else {
                processingEvent.setStatus(OutboxStatus.FAILED);
                processingEvent.setNextRetryAt(retryPolicy.nextRetryAt(newAttemptCount));
                log.warn("Falha ao publicar evento. attempt={}/{}, nextRetry={}, eventId={}",
                        newAttemptCount, maxAttempts, processingEvent.getNextRetryAt(),
                        processingEvent.getEventId(), ex);
            }

            outboxEventPort.save(processingEvent);
        }
    }

    private void createMissingPendingTargets(UUID eventId) {
        List<PendingTargetStatus> existing = pendingTargetStatusPort.findByEventId(eventId);
        Set<String> existingTargets = existing.stream()
                .map(PendingTargetStatus::getTargetService)
                .collect(Collectors.toSet());

        if (!existingTargets.contains(TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO)) {
            pendingTargetStatusPort.save(new PendingTargetStatus(
                    null,
                    eventId,
                    TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO,
                    Status.PENDING));
        }

        if (!existingTargets.contains(TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO)) {
            pendingTargetStatusPort.save(new PendingTargetStatus(
                    null,
                    eventId,
                    TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO,
                    Status.PENDING));
        }
    }
}
