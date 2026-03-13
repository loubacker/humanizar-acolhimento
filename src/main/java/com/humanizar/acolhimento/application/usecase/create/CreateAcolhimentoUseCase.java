package com.humanizar.acolhimento.application.usecase.create;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;

@Service
public class CreateAcolhimentoUseCase {

    private final AcolhimentoPort acolhimentoPort;

    public CreateAcolhimentoUseCase(AcolhimentoPort acolhimentoPort) {
        this.acolhimentoPort = acolhimentoPort;
    }

    @Transactional
    public Acolhimento execute(Acolhimento acolhimento) {
        return acolhimentoPort.save(acolhimento);
    }

    public boolean existsByPatientId(java.util.UUID patientId) {
        return acolhimentoPort.existsByPatientId(patientId);
    }
}
