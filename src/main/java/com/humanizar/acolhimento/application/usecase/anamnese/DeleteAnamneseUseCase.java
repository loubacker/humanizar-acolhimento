package com.humanizar.acolhimento.application.usecase.anamnese;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;

@Service
public class DeleteAnamneseUseCase {

    private final AcolhimentoPort acolhimentoPort;

    public DeleteAnamneseUseCase(AcolhimentoPort acolhimentoPort) {
        this.acolhimentoPort = acolhimentoPort;
    }

    @Transactional
    public void execute(UUID nucleoPatientId) {
        acolhimentoPort.deleteByNucleoPatientId(nucleoPatientId);
    }
}
