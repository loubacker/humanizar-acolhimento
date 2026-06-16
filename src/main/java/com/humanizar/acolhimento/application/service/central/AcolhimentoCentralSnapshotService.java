package com.humanizar.acolhimento.application.service.central;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.http.dto.central.PendingCentralSnapshotDTO;
import com.humanizar.acolhimento.application.http.dto.central.PendingTargetStatusDTO;
import com.humanizar.acolhimento.application.http.mapper.central.PendingTargetStatusDtoMapper;
import com.humanizar.acolhimento.application.usecase.central.FindPendingByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.central.FindTargetsByEventIdUseCase;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;

@Service
public class AcolhimentoCentralSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoCentralSnapshotService.class);

    private final FindPendingByEventIdUseCase findPendingByEventIdUseCase;
    private final FindTargetsByEventIdUseCase findTargetsByEventIdUseCase;
    private final PendingTargetStatusDtoMapper pendingTargetStatusDtoMapper;

    public AcolhimentoCentralSnapshotService(
            FindPendingByEventIdUseCase findPendingByEventIdUseCase,
            FindTargetsByEventIdUseCase findTargetsByEventIdUseCase,
            PendingTargetStatusDtoMapper pendingTargetStatusDtoMapper) {
        this.findPendingByEventIdUseCase = findPendingByEventIdUseCase;
        this.findTargetsByEventIdUseCase = findTargetsByEventIdUseCase;
        this.pendingTargetStatusDtoMapper = pendingTargetStatusDtoMapper;
    }

    public Optional<PendingCentralSnapshotDTO> execute(UUID eventId) {
        log.info("Buscando snapshot da execucao para eventId={}", eventId);

        return findPendingByEventIdUseCase.execute(eventId)
                .map(this::toSnapshotDTO);
    }

    private PendingCentralSnapshotDTO toSnapshotDTO(PendingAcolhimento pending) {
        List<PendingTargetStatusDTO> targets = pendingTargetStatusDtoMapper.toDTOList(
                findTargetsByEventIdUseCase.execute(pending.getEventId()));

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
