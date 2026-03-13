package com.humanizar.acolhimento.application.usecase.create;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientResponsavelPort;

@Service
public class CreateNucleoPatientResponsavelUseCase {

    private final NucleoPatientResponsavelPort nucleoPatientResponsavelPort;

    public CreateNucleoPatientResponsavelUseCase(NucleoPatientResponsavelPort nucleoPatientResponsavelPort) {
        this.nucleoPatientResponsavelPort = nucleoPatientResponsavelPort;
    }

    @Transactional
    public List<NucleoPatientResponsavel> execute(List<NucleoPatientResponsavel> responsaveis) {
        return nucleoPatientResponsavelPort.saveAll(responsaveis);
    }
}
