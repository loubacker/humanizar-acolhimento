package com.humanizar.acolhimento.application.usecase.retrieve;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;

@Service
public class RetrieveNucleoPatientUseCase {

    private final NucleoPatientPort nucleoPatientPort;

    public RetrieveNucleoPatientUseCase(NucleoPatientPort nucleoPatientPort) {
        this.nucleoPatientPort = nucleoPatientPort;
    }

    @Transactional(readOnly = true)
    public List<NucleoPatient> execute(UUID patientId) {
        return nucleoPatientPort.findByPatientId(patientId);
    }
}
