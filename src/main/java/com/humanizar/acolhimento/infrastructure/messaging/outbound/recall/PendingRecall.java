package com.humanizar.acolhimento.infrastructure.messaging.outbound.recall;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@Component
public class PendingRecall {

    private static final Logger log = LoggerFactory.getLogger(PendingRecall.class);

    private final PendingTargetStatusPort pendingTargetStatusPort;
    private final PendingAcolhimentoPort pendingAcolhimentoPort;
    private final OutboxEventPort outboxEventPort;
    private final RecallPolicy recallPolicy;

    public PendingRecall(
            PendingTargetStatusPort pendingTargetStatusPort,
            PendingAcolhimentoPort pendingAcolhimentoPort,
            OutboxEventPort outboxEventPort,
            RecallPolicy recallPolicy) {
        this.pendingTargetStatusPort = pendingTargetStatusPort;
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
        this.outboxEventPort = outboxEventPort;
        this.recallPolicy = recallPolicy;
    }

    @Transactional
    public void recall(int batchSize) {
        List<PendingTargetStatus> staleTargets = pendingTargetStatusPort.findStaleTargets(
                Status.PENDING, LocalDateTime.now(), batchSize);
        if (staleTargets.isEmpty()) {
            return;
        }

        for (PendingTargetStatus target : staleTargets) {
            recallTarget(target);
        }
    }

    private void recallTarget(PendingTargetStatus target) {
        UUID eventId = target.getEventId();

        PendingAcolhimento pending = pendingAcolhimentoPort.findByEventId(eventId).orElse(null);
        if (pending == null || pending.getStatus() != Status.PENDING) {
            return;
        }

        int attempts = target.getRecallAttempts() != null ? target.getRecallAttempts() : 0;

        switch (reDrive(eventId)) {
            case RE_DRIVEN -> trackReDrive(target, attempts);
            case RELAY_BUSY -> log.debug(
                    "Outbox em processamento pelo relay; recall adiada. eventId={}", eventId);
            case NO_OUTBOX -> escalate(target, pending, attempts, "recall_no_outbox");
        }
    }

    private ReDriveResult reDrive(UUID eventId) {
        OutboxEvent locked = outboxEventPort.findByEventIdForUpdate(eventId).orElse(null);
        if (locked != null) {
            locked.setStatus(OutboxStatus.FAILED);
            locked.setNextRetryAt(LocalDateTime.now());
            locked.setLockedBy(null);
            outboxEventPort.save(locked);
            return ReDriveResult.RE_DRIVEN;
        }

        if (outboxEventPort.findByEventId(eventId).isPresent()) {
            return ReDriveResult.RELAY_BUSY;
        }
        return ReDriveResult.NO_OUTBOX;
    }

    private void trackReDrive(PendingTargetStatus target, int attempts) {
        int newAttempts = attempts + 1;
        target.setRecallAttempts(newAttempts);
        target.setLastRecallAt(LocalDateTime.now());
        target.setNextRecallAt(recallPolicy.nextRecallAt(newAttempts));
        pendingTargetStatusPort.save(target);
        log.info("Comando re-dirigido por recall. eventId={}, targetService={}, attempt={}",
                target.getEventId(), target.getTargetService(), newAttempts);
    }

    private void escalate(PendingTargetStatus target, PendingAcolhimento pending, int attempts, String marker) {
        target.setStatus(Status.ERROR);
        pendingTargetStatusPort.save(target);

        pending.setStatus(Status.ERROR);
        pendingAcolhimentoPort.save(pending);

        log.error("{} eventId={}, targetService={}, attempts={}",
                marker, target.getEventId(), target.getTargetService(), attempts);
    }

    private enum ReDriveResult {
        RE_DRIVEN,
        RELAY_BUSY,
        NO_OUTBOX
    }
}
