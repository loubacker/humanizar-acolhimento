package com.humanizar.acolhimento.application.usecase.central;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;

@Component
public class FindPendingByEventIdUseCase {

    private static final Logger log = LoggerFactory.getLogger(FindPendingByEventIdUseCase.class);

    private final PendingAcolhimentoPort pendingAcolhimentoPort;

    public FindPendingByEventIdUseCase(PendingAcolhimentoPort pendingAcolhimentoPort) {
        this.pendingAcolhimentoPort = pendingAcolhimentoPort;
    }

    public Optional<PendingAcolhimento> execute(UUID eventId) {
        log.debug("Buscando execucao pendente para eventId={}", eventId);
        return pendingAcolhimentoPort.findByEventId(eventId);
    }
}
