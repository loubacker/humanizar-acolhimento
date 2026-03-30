package com.humanizar.acolhimento.application.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.inbound.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoMappingResult;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.inbound.mapper.InboundAcolhimentoMapper;
import com.humanizar.acolhimento.application.inbound.mapper.InboundContextMapper;
import com.humanizar.acolhimento.application.usecase.create.CreateAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreateNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreateNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreateOutboxCommandUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.DeleteCreateCompensationUseCase;
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoCreateResponseDTO;

@Service
public class AcolhimentoCreateService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoCreateService.class);

    private final InboundContextMapper inboundContextMapper;
    private final InboundAcolhimentoMapper inboundAcolhimentoMapper;
    private final ObjectMapper objectMapper;
    private final Executor inboundExecutor;
    private final CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;
    private final CreateAcolhimentoUseCase createAcolhimentoUseCase;
    private final CreateNucleoPatientUseCase createNucleoPatientUseCase;
    private final CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase;
    private final CreateOutboxCommandUseCase createOutboxCommandUseCase;
    private final DeleteCreateCompensationUseCase deleteCreateCompensationUseCase;
    private final MarkPendingCreateUseCase markPendingCreateUseCase;

    public AcolhimentoCreateService(
            InboundContextMapper inboundContextMapper,
            InboundAcolhimentoMapper inboundAcolhimentoMapper,
            ObjectMapper objectMapper,
            @Qualifier("inboundExecutor") Executor inboundExecutor,
            CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase,
            CreateAcolhimentoUseCase createAcolhimentoUseCase,
            CreateNucleoPatientUseCase createNucleoPatientUseCase,
            CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase,
            CreateOutboxCommandUseCase createOutboxCommandUseCase,
            DeleteCreateCompensationUseCase deleteCreateCompensationUseCase,
            MarkPendingCreateUseCase markPendingCreateUseCase) {
        this.inboundContextMapper = inboundContextMapper;
        this.inboundAcolhimentoMapper = inboundAcolhimentoMapper;
        this.objectMapper = objectMapper;
        this.inboundExecutor = inboundExecutor;
        this.createPendingAcolhimentoUseCase = createPendingAcolhimentoUseCase;
        this.createAcolhimentoUseCase = createAcolhimentoUseCase;
        this.createNucleoPatientUseCase = createNucleoPatientUseCase;
        this.createNucleoPatientResponsavelUseCase = createNucleoPatientResponsavelUseCase;
        this.createOutboxCommandUseCase = createOutboxCommandUseCase;
        this.deleteCreateCompensationUseCase = deleteCreateCompensationUseCase;
        this.markPendingCreateUseCase = markPendingCreateUseCase;
    }

    public AcolhimentoCreateResponseDTO register(InboundEnvelopeDTO<InboundAcolhimentoDTO> envelope) {
        InboundContextDTO<InboundAcolhimentoDTO> context = inboundContextMapper.fromEnvelop(envelope);
        UUID correlationId = context.envelop().correlationId();
        String correlationIdText = correlationId != null ? correlationId.toString() : null;

        InboundAcolhimentoDTO payload = inboundAcolhimentoMapper.toCreatePayload(context.payload());
        UUID patientId = payload.patientId();
        InboundAcolhimentoMappingResult mappingResult = inboundAcolhimentoMapper.mapCreate(payload, correlationIdText);

        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                correlationId,
                patientId,
                OperationType.CREATE,
                serializeSnapshot(payload, correlationIdText));

        if (createAcolhimentoUseCase.existsByPatientId(patientId)) {
            safeMarkPendingAsError(pending.getEventId());
            throw duplicatePatientException(correlationIdText, patientId);
        }

        try {
            executeParallelStage(mappingResult);
            createNucleoPatientResponsavelUseCase.execute(mappingResult.nucleoPatientResponsaveis());
        } catch (Exception ex) {
            safeMarkPendingAsError(pending.getEventId());
            compensateLocalState(mappingResult);
            if (isDuplicateException(ex)) {
                throw duplicatePatientException(correlationIdText, patientId);
            }
            throw unwrap(ex, correlationIdText);
        }

        try {
            createOutboxCommandUseCase.execute(context.envelop(), pending.getEventId(), mappingResult);
        } catch (Exception ex) {
            safeMarkPendingAsError(pending.getEventId());
            throw unwrap(ex, correlationIdText);
        }

        return new AcolhimentoCreateResponseDTO(
                "Acolhimento Criado com Sucesso para Paciente " + patientId,
                patientId,
                correlationId);
    }

    private void executeParallelStage(InboundAcolhimentoMappingResult mappingResult) {
        CompletableFuture<Void> saveAcolhimento = runAsync(() -> {
            createAcolhimentoUseCase.execute(mappingResult.acolhimento());
            return null;
        });

        CompletableFuture<Void> saveNucleoPatient = runAsync(() -> {
            createNucleoPatientUseCase.execute(mappingResult.nucleoPatients());
            return null;
        });

        CompletableFuture.allOf(saveAcolhimento, saveNucleoPatient).join();
    }

    private <T> CompletableFuture<T> runAsync(Supplier<T> action) {
        return CompletableFuture.supplyAsync(action, inboundExecutor);
    }

    private void compensateLocalState(InboundAcolhimentoMappingResult mappingResult) {
        UUID acolhimentoId = mappingResult.acolhimento() != null ? mappingResult.acolhimento().getId() : null;
        java.util.List<UUID> nucleoPatientIds = mappingResult.nucleoPatients().stream()
                .map(nucleoPatient -> nucleoPatient != null ? nucleoPatient.getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        java.util.List<UUID> nucleoPatientResponsavelIds = mappingResult.nucleoPatientResponsaveis().stream()
                .map(responsavel -> responsavel != null ? responsavel.getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        try {
            deleteCreateCompensationUseCase.execute(acolhimentoId, nucleoPatientIds, nucleoPatientResponsavelIds);
        } catch (Exception ex) {
            log.error("Falha na compensacao local de create. acolhimentoId={}, nucleoPatientIds={}, responsavelIds={}",
                    acolhimentoId, nucleoPatientIds, nucleoPatientResponsavelIds, ex);
        }
    }

    private void safeMarkPendingAsError(java.util.UUID eventId) {
        try {
            markPendingCreateUseCase.markError(eventId);
        } catch (Exception ex) {
            log.error("Falha ao marcar pending_acolhimento como ERROR. eventId={}", eventId, ex);
        }
    }

    private AcolhimentoException unwrap(Exception ex, String correlationId) {
        Throwable root = ex;
        if (ex instanceof CompletionException completionException && completionException.getCause() != null) {
            root = completionException.getCause();
        }

        if (root instanceof AcolhimentoException acolhimentoException) {
            return acolhimentoException;
        }

        if (isDuplicateException(root)) {
            return new AcolhimentoException(
                    ReasonCode.DUPLICATE_PATIENT,
                    correlationId,
                    "Acolhimento ja existe para o patientId informado");
        }

        String message = (root != null && root.getMessage() != null)
                ? root.getMessage()
                : "Falha no pipeline de create";

        return new AcolhimentoException(
                ReasonCode.PERSISTENCE_FAILURE,
                correlationId,
                message);
    }

    private AcolhimentoException duplicatePatientException(String correlationId, UUID patientId) {
        return new AcolhimentoException(
                ReasonCode.DUPLICATE_PATIENT,
                correlationId,
                "Acolhimento ja existe para patientId=" + patientId);
    }

    private boolean isDuplicateException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.sql.SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return true;
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("uk_acolhimento_patient_id")
                        || normalized.contains("uk_patient_nucleo")
                        || normalized.contains("duplicate key")
                        || normalized.contains("restrição de unicidade")
                        || normalized.contains("restricao de unicidade")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String serializeSnapshot(
            InboundAcolhimentoDTO payload,
            String correlationId) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Falha ao serializar payloadSnapshot de pending_acolhimento");
        }
    }
}
