package com.humanizar.acolhimento.application.usecase.anamnese;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;

@Service
public class RetrieveAnamneseUseCase {

    private final AcolhimentoPort acolhimentoPort;

    public RetrieveAnamneseUseCase(AcolhimentoPort acolhimentoPort) {
        this.acolhimentoPort = acolhimentoPort;
    }

    @Transactional(readOnly = true)
    public Optional<Acolhimento> execute(UUID nucleoPatientId) {
        return acolhimentoPort.findByNucleoPatientId(nucleoPatientId);
    }

    @Transactional(readOnly = true)
    public List<Acolhimento> executeBatch(List<UUID> nucleoPatientIds) {
        return acolhimentoPort.findByNucleoPatientIdIn(nucleoPatientIds);
    }
}
