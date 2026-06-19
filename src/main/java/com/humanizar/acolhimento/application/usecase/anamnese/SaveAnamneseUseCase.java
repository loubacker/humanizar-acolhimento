package com.humanizar.acolhimento.application.usecase.anamnese;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;

@Service
public class SaveAnamneseUseCase {

    private final AcolhimentoPort acolhimentoPort;
    private final NucleoPatientPort nucleoPatientPort;

    public SaveAnamneseUseCase(AcolhimentoPort acolhimentoPort, NucleoPatientPort nucleoPatientPort) {
        this.acolhimentoPort = acolhimentoPort;
        this.nucleoPatientPort = nucleoPatientPort;
    }

    @Transactional
    public Acolhimento execute(Acolhimento anamnese) {
        Acolhimento saved = acolhimentoPort.save(anamnese);
        nucleoPatientPort.updateAnamneseStatus(saved.getNucleoPatientId(), AnamneseStatus.ENTREGUE);
        return saved;
    }

    public boolean existsByNucleoPatientId(java.util.UUID nucleoPatientId) {
        return acolhimentoPort.existsByNucleoPatientId(nucleoPatientId);
    }
}
