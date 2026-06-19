package com.humanizar.acolhimento.application.usecase.anamnese;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;

@Service
public class DeleteAnamneseUseCase {

    private final AcolhimentoPort acolhimentoPort;
    private final NucleoPatientPort nucleoPatientPort;

    public DeleteAnamneseUseCase(AcolhimentoPort acolhimentoPort, NucleoPatientPort nucleoPatientPort) {
        this.acolhimentoPort = acolhimentoPort;
        this.nucleoPatientPort = nucleoPatientPort;
    }

    @Transactional
    public void execute(UUID nucleoPatientId) {
        acolhimentoPort.deleteByNucleoPatientId(nucleoPatientId);
        nucleoPatientPort.updateAnamneseStatus(nucleoPatientId, AnamneseStatus.PENDENTE);
    }
}
