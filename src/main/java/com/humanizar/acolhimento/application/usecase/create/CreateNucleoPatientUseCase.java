package com.humanizar.acolhimento.application.usecase.create;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;

@Service
public class CreateNucleoPatientUseCase {

    private final NucleoPatientPort nucleoPatientPort;

    public CreateNucleoPatientUseCase(NucleoPatientPort nucleoPatientPort) {
        this.nucleoPatientPort = nucleoPatientPort;
    }

    @Transactional
    public List<NucleoPatient> execute(List<NucleoPatient> nucleoPatients) {
        return nucleoPatientPort.saveAll(nucleoPatients);
    }
}
