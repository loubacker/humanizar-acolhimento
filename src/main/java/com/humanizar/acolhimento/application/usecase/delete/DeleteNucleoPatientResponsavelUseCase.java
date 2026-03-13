package com.humanizar.acolhimento.application.usecase.delete;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientResponsavelPort;

@Service
public class DeleteNucleoPatientResponsavelUseCase {

    private final NucleoPatientResponsavelPort nucleoPatientResponsavelPort;

    public DeleteNucleoPatientResponsavelUseCase(
            NucleoPatientResponsavelPort nucleoPatientResponsavelPort) {
        this.nucleoPatientResponsavelPort = nucleoPatientResponsavelPort;
    }

    @Transactional
    public void execute(UUID patientId) {
        nucleoPatientResponsavelPort.deleteByPatientId(patientId);
    }
}
