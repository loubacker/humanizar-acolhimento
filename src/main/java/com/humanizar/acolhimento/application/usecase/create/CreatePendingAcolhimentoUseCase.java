package com.humanizar.acolhimento.application.usecase.create;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;

@Service
public class CreatePendingAcolhimentoUseCase {

    private final PendingAcolhimentoPort pendingAcolhimentoPort;

    public CreatePendingAcolhimentoUseCase(PendingAcolhimentoPort pendingAcolhimentoPort) {
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
    }

    @Transactional
    public PendingAcolhimento execute(
            UUID correlationId,
            UUID patientId,
            OperationType operationType,
            String payloadSnapshot) {
        PendingAcolhimento pending = PendingAcolhimento.builder()
                .eventId(UUID.randomUUID())
                .correlationId(correlationId)
                .patientId(patientId)
                .operationType(operationType)
                .status(Status.PENDING)
                .payloadSnapshot(payloadSnapshot)
                .createdAt(LocalDateTime.now())
                .build();
        return pendingAcolhimentoPort.save(pending);
    }
}
