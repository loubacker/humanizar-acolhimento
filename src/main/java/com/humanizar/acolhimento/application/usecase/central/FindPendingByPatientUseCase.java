package com.humanizar.acolhimento.application.usecase.central;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;

@Component
public class FindPendingByPatientUseCase {

    private static final Logger log = LoggerFactory.getLogger(FindPendingByPatientUseCase.class);

    private final PendingAcolhimentoPort pendingAcolhimentoPort;

    public FindPendingByPatientUseCase(PendingAcolhimentoPort pendingAcolhimentoPort) {
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
    }

    public Page<PendingAcolhimento> execute(UUID patientId, Pageable pageable) {
        log.debug("Buscando execucoes pendentes para patientId={}, page={}, size={}",
                patientId, pageable.getPageNumber(), pageable.getPageSize());
        return pendingAcolhimentoPort.findByPatientId(patientId, pageable);
    }
}
