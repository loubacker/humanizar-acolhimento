package com.humanizar.acolhimento.application.usecase.callback;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@Service
public class UpdateCallbackUseCase {

    private final PendingTargetStatusPort pendingTargetStatusPort;

    public UpdateCallbackUseCase(PendingTargetStatusPort pendingTargetStatusPort) {
        this.pendingTargetStatusPort = pendingTargetStatusPort;
    }

    @Transactional
    public void execute(UUID eventId, String targetService, Status status) {
        PendingTargetStatus entity = pendingTargetStatusPort
                .findByEventIdAndTargetService(eventId, targetService)
                .orElse(new PendingTargetStatus(null, eventId, targetService, status));
        entity.setStatus(status);
        pendingTargetStatusPort.save(entity);
    }
}
