package com.humanizar.acolhimento.application.usecase.create;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;

@Service
public class MarkPendingCreateUseCase {

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

    private void updateStatus(UUID eventId, Status status) {
        PendingAcolhimento pending = pendingAcolhimentoPort.findByEventId(eventId)
                .orElseThrow(() -> new AcolhimentoException(
                        ReasonCode.PERSISTENCE_FAILURE,
                        null,
                        "pending_acolhimento nao encontrado para eventId=" + eventId));
        pending.setStatus(status);
        pendingAcolhimentoPort.save(pending);
    }
}
