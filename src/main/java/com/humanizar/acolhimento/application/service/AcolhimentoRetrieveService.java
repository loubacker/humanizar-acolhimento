package com.humanizar.acolhimento.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.http.dto.anamnese.AcolhimentoListResponseDTO;
import com.humanizar.acolhimento.application.usecase.snapshot.BuildPatientSnapshotUseCase;

@Service
public class AcolhimentoRetrieveService {

    private final BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;

    public AcolhimentoRetrieveService(BuildPatientSnapshotUseCase buildPatientSnapshotUseCase) {
        this.buildPatientSnapshotUseCase = buildPatientSnapshotUseCase;
    }

    public AcolhimentoListResponseDTO findByPatientId(UUID patientId) {
        return buildPatientSnapshotUseCase.execute(patientId);
    }
}
