package com.humanizar.acolhimento.application.usecase.delete;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;

@Service
public class DeleteNucleoPatientUseCase {

    private final NucleoPatientPort nucleoPatientPort;

    public DeleteNucleoPatientUseCase(NucleoPatientPort nucleoPatientPort) {
        this.nucleoPatientPort = nucleoPatientPort;
    }

    @Transactional
    public void execute(UUID patientId) {
        nucleoPatientPort.deleteByPatientId(patientId);
    }
}
