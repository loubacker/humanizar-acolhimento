package com.humanizar.acolhimento.application.usecase.create;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;

@Service
public class MarkPendingCreateUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkPendingCreateUseCase.class);

    private final PendingAcolhimentoPort pendingAcolhimentoPort;

    public MarkPendingCreateUseCase(PendingAcolhimentoPort pendingAcolhimentoPort) {
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
    }

    public void markSuccess(UUID eventId) {
        updateStatus(eventId, Status.SUCCESS);
    }

    public void markError(UUID eventId) {
        updateStatus(eventId, Status.ERROR);
    }

    public void markErrorQuietly(UUID eventId) {
        try {
            markError(eventId);
        } catch (Exception ex) {
            log.error("Falha ao marcar pending_acolhimento como ERROR. eventId={}", eventId, ex);
        }
    }

    private void updateStatus(UUID eventId, Status status) {
        PendingAcolhimento pending = pendingAcolhimentoPort.getByEventId(eventId);
        pending.setStatus(status);
        pendingAcolhimentoPort.save(pending);
    }
}
