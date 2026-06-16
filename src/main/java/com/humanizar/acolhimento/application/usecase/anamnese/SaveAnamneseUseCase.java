package com.humanizar.acolhimento.application.usecase.anamnese;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;

@Service
public class SaveAnamneseUseCase {

    private final AcolhimentoPort acolhimentoPort;

    public SaveAnamneseUseCase(AcolhimentoPort acolhimentoPort) {
        this.acolhimentoPort = acolhimentoPort;
    }

    @Transactional
    public Acolhimento execute(Acolhimento anamnese) {
        return acolhimentoPort.save(anamnese);
    }

    public boolean existsByNucleoPatientId(java.util.UUID nucleoPatientId) {
        return acolhimentoPort.existsByNucleoPatientId(nucleoPatientId);
    }
}
