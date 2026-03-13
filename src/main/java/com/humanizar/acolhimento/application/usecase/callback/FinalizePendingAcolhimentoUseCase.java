package com.humanizar.acolhimento.application.usecase.callback;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@Service
public class FinalizePendingAcolhimentoUseCase {

    private final PendingAcolhimentoPort pendingAcolhimentoPort;
    private final PendingTargetStatusPort pendingTargetStatusPort;

    public FinalizePendingAcolhimentoUseCase(
            PendingAcolhimentoPort pendingAcolhimentoPort,
            PendingTargetStatusPort pendingTargetStatusPort) {
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
        this.pendingTargetStatusPort = pendingTargetStatusPort;
    }

    @Transactional
    public void execute(UUID eventId) {
        PendingAcolhimento pending = pendingAcolhimentoPort.findByEventId(eventId).orElse(null);
        if (pending == null || pending.getStatus() != Status.PENDING) {
            return;
        }

        List<PendingTargetStatus> targetStatuses = pendingTargetStatusPort.findByEventId(eventId);
        if (targetStatuses == null || targetStatuses.isEmpty()) {
            return;
        }

        if (targetStatuses.stream().anyMatch(target -> target.getStatus() == Status.ERROR)) {
            pending.setStatus(Status.ERROR);
            pendingAcolhimentoPort.save(pending);
            return;
        }

        boolean allSuccess = targetStatuses.stream().allMatch(target -> target.getStatus() == Status.SUCCESS);
        if (allSuccess) {
            pending.setStatus(Status.SUCCESS);
            pendingAcolhimentoPort.save(pending);
        }
    }
}
