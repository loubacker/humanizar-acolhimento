package com.humanizar.acolhimento.application.usecase.create;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientResponsavelPort;

@Service
public class DeleteCreateCompensationUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteCreateCompensationUseCase.class);

    private final NucleoPatientResponsavelPort nucleoPatientResponsavelPort;
    private final NucleoPatientPort nucleoPatientPort;
    private final AcolhimentoPort acolhimentoPort;

    public DeleteCreateCompensationUseCase(
            NucleoPatientResponsavelPort nucleoPatientResponsavelPort,
            NucleoPatientPort nucleoPatientPort,
            AcolhimentoPort acolhimentoPort) {
        this.nucleoPatientResponsavelPort = nucleoPatientResponsavelPort;
        this.nucleoPatientPort = nucleoPatientPort;
        this.acolhimentoPort = acolhimentoPort;
    }

    public void execute(
            UUID acolhimentoId,
            List<UUID> nucleoPatientIds,
            List<UUID> nucleoPatientResponsavelIds) {
        try {
            nucleoPatientResponsavelPort.deleteByIds(nucleoPatientResponsavelIds);
        } catch (Exception ex) {
            log.error("Falha na compensacao de nucleo_patient_responsavel. ids={}", nucleoPatientResponsavelIds, ex);
        }

        try {
            nucleoPatientPort.deleteByIds(nucleoPatientIds);
        } catch (Exception ex) {
            log.error("Falha na compensacao de nucleo_patient. ids={}", nucleoPatientIds, ex);
        }

        try {
            acolhimentoPort.deleteById(acolhimentoId);
        } catch (Exception ex) {
            log.error("Falha na compensacao de acolhimento. id={}", acolhimentoId, ex);
        }
    }
}
