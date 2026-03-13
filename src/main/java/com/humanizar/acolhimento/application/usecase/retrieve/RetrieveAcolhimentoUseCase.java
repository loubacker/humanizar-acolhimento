package com.humanizar.acolhimento.application.usecase.retrieve;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;

@Service
public class RetrieveAcolhimentoUseCase {

    private final AcolhimentoPort acolhimentoPort;

    public RetrieveAcolhimentoUseCase(AcolhimentoPort acolhimentoPort) {
        this.acolhimentoPort = acolhimentoPort;
    }

    @Transactional(readOnly = true)
    public Acolhimento execute(UUID patientId, String correlationId) {
        return acolhimentoPort.findByPatientId(patientId)
                .orElseThrow(() -> new AcolhimentoException(
                        ReasonCode.PATIENT_NOT_FOUND,
                        correlationId,
                        "Paciente nao encontrado para patientId=" + patientId));
    }
}
