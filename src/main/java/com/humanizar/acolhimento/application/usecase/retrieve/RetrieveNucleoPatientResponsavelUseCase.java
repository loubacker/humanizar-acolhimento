package com.humanizar.acolhimento.application.usecase.retrieve;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientResponsavelPort;

@Service
public class RetrieveNucleoPatientResponsavelUseCase {

    private final NucleoPatientResponsavelPort nucleoPatientResponsavelPort;

    public RetrieveNucleoPatientResponsavelUseCase(
            NucleoPatientResponsavelPort nucleoPatientResponsavelPort) {
        this.nucleoPatientResponsavelPort = nucleoPatientResponsavelPort;
    }

    @Transactional(readOnly = true)
    public List<NucleoPatientResponsavel> execute(UUID nucleoPatientId) {
        return nucleoPatientResponsavelPort.findByNucleoPatientId(nucleoPatientId);
    }
}
