package com.humanizar.acolhimento.application.service.central;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.outbound.dto.central.PendingCentralListDTO;
import com.humanizar.acolhimento.application.outbound.dto.central.PendingCentralPageDTO;
import com.humanizar.acolhimento.application.outbound.dto.central.PendingTargetStatusDTO;
import com.humanizar.acolhimento.application.usecase.central.FindPendingByPatientUseCase;
import com.humanizar.acolhimento.application.usecase.central.FindTargetsByEventIdUseCase;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;

@Service
public class AcolhimentoCentralListService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoCentralListService.class);

    private final FindPendingByPatientUseCase findPendingByPatientUseCase;
    private final FindTargetsByEventIdUseCase findTargetsByEventIdUseCase;

    public AcolhimentoCentralListService(
            FindPendingByPatientUseCase findPendingByPatientUseCase,
            FindTargetsByEventIdUseCase findTargetsByEventIdUseCase) {
        this.findPendingByPatientUseCase = findPendingByPatientUseCase;
        this.findTargetsByEventIdUseCase = findTargetsByEventIdUseCase;
    }

    public PendingCentralPageDTO execute(UUID patientId, int page, int size) {
        log.info("Listando execucoes da central para patientId={}, page={}, size={}", patientId, page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PendingAcolhimento> pendingPage = findPendingByPatientUseCase.execute(patientId, pageable);

        List<PendingCentralListDTO> data = pendingPage.getContent().stream()
                .map(this::toListDTO)
                .toList();

        log.info("Retornando {} execucoes (pagina {} de {}) para patientId={}",
                data.size(), page, pendingPage.getTotalPages(), patientId);

        return new PendingCentralPageDTO(
                data,
                pendingPage.getNumber(),
                pendingPage.getSize(),
                pendingPage.getTotalPages(),
                pendingPage.getTotalElements());
    }

    private PendingCentralListDTO toListDTO(PendingAcolhimento pending) {
        List<PendingTargetStatusDTO> targets = findTargetsByEventIdUseCase
                .execute(pending.getEventId())
                .stream()
                .map(t -> new PendingTargetStatusDTO(
                        t.getTargetService(),
                        t.getStatus().name()))
                .toList();

        return new PendingCentralListDTO(
                pending.getEventId(),
                pending.getCorrelationId(),
                pending.getPatientId(),
                pending.getOperationType().name(),
                pending.getStatus().name(),
                pending.getCreatedAt(),
                targets);
    }
}
