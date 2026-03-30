package com.humanizar.acolhimento.application.usecase.delete;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;

@Component
public class ValidateDeleteProgressUseCase {

    private final PendingAcolhimentoPort pendingAcolhimentoPort;

    public ValidateDeleteProgressUseCase(PendingAcolhimentoPort pendingAcolhimentoPort) {
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
    }

    public void execute(UUID patientId, String correlationId) {
        boolean hasDeleteInProgress = pendingAcolhimentoPort.checkDeleteStatusByPatientId(
                patientId,
                OperationType.DELETE,
                Status.PENDING);

        if (hasDeleteInProgress) {
            throw new AcolhimentoException(
                    ReasonCode.DELETE_IN_PROGRESS,
                    correlationId,
                    "Ja existe operacao DELETE pendente para patientId=" + patientId);
        }
    }
}
