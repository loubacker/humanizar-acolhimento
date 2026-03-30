package com.humanizar.acolhimento.application.service.central;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.outbound.dto.central.PendingCentralSnapshotDTO;
import com.humanizar.acolhimento.application.outbound.dto.central.PendingTargetStatusDTO;
import com.humanizar.acolhimento.application.usecase.central.FindPendingByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.central.FindTargetsByEventIdUseCase;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;

@Service
public class AcolhimentoCentralSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoCentralSnapshotService.class);

    private final FindPendingByEventIdUseCase findPendingByEventIdUseCase;
    private final FindTargetsByEventIdUseCase findTargetsByEventIdUseCase;

    public AcolhimentoCentralSnapshotService(
            FindPendingByEventIdUseCase findPendingByEventIdUseCase,
            FindTargetsByEventIdUseCase findTargetsByEventIdUseCase) {
        this.findPendingByEventIdUseCase = findPendingByEventIdUseCase;
        this.findTargetsByEventIdUseCase = findTargetsByEventIdUseCase;
    }

    public Optional<PendingCentralSnapshotDTO> execute(UUID eventId) {
        log.info("Buscando snapshot da execucao para eventId={}", eventId);

        return findPendingByEventIdUseCase.execute(eventId)
                .map(this::toSnapshotDTO);
    }

    private PendingCentralSnapshotDTO toSnapshotDTO(PendingAcolhimento pending) {
        List<PendingTargetStatusDTO> targets = findTargetsByEventIdUseCase
                .execute(pending.getEventId())
                .stream()
                .map(t -> new PendingTargetStatusDTO(
                        t.getTargetService(),
                        t.getStatus().name()))
                .toList();

        return new PendingCentralSnapshotDTO(
                pending.getEventId(),
                pending.getCorrelationId(),
                pending.getPatientId(),
                pending.getOperationType().name(),
                pending.getStatus().name(),
                pending.getCreatedAt(),
                pending.getPayloadSnapshot(),
                targets);
    }
}
