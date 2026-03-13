package com.humanizar.acolhimento.application.usecase.delete;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;

@Service
public class DeleteAcolhimentoUseCase {

    private final AcolhimentoPort acolhimentoPort;

    public DeleteAcolhimentoUseCase(AcolhimentoPort acolhimentoPort) {
        this.acolhimentoPort = acolhimentoPort;
    }

    @Transactional
    public void execute(UUID patientId) {
        acolhimentoPort.deleteByPatientId(patientId);
    }
}
