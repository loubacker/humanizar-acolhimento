package com.humanizar.acolhimento.application.service;

import java.util.concurrent.CompletionException;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.http.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoMapResultDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.mapper.InboundAcolhimentoMapper;
import com.humanizar.acolhimento.application.http.mapper.InboundContextMapper;
import com.humanizar.acolhimento.application.usecase.command.OutboxCommandUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreateNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreateNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.DeleteCreateCompensationUseCase;
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.application.usecase.exception.PipelineExceptionUseCase;
import com.humanizar.acolhimento.application.usecase.snapshot.BuildPatientSnapshotUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@Service
public class AcolhimentoCreateService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoCreateService.class);
    private static final String EMPTY_SNAPSHOT = "{}";

    private final InboundContextMapper inboundContextMapper;
    private final InboundAcolhimentoMapper inboundAcolhimentoMapper;
    private final CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;
    private final CreateNucleoPatientUseCase createNucleoPatientUseCase;
    private final CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase;
    private final OutboxCommandUseCase outboxCommandUseCase;
    private final DeleteCreateCompensationUseCase deleteCreateCompensationUseCase;
    private final MarkPendingCreateUseCase markPendingCreateUseCase;
    private final BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;
    private final NucleoPatientPort nucleoPatientPort;
    private final PipelineExceptionUseCase pipelineExceptionUseCase;

    public AcolhimentoCreateService(
            InboundContextMapper inboundContextMapper,
            InboundAcolhimentoMapper inboundAcolhimentoMapper,
            CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase,
            CreateNucleoPatientUseCase createNucleoPatientUseCase,
            CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase,
            OutboxCommandUseCase outboxCommandUseCase,
            DeleteCreateCompensationUseCase deleteCreateCompensationUseCase,
            MarkPendingCreateUseCase markPendingCreateUseCase,
            BuildPatientSnapshotUseCase buildPatientSnapshotUseCase,
            NucleoPatientPort nucleoPatientPort,
            PipelineExceptionUseCase pipelineExceptionUseCase) {
        this.inboundContextMapper = inboundContextMapper;
        this.inboundAcolhimentoMapper = inboundAcolhimentoMapper;
        this.createPendingAcolhimentoUseCase = createPendingAcolhimentoUseCase;
        this.createNucleoPatientUseCase = createNucleoPatientUseCase;
        this.createNucleoPatientResponsavelUseCase = createNucleoPatientResponsavelUseCase;
        this.outboxCommandUseCase = outboxCommandUseCase;
        this.deleteCreateCompensationUseCase = deleteCreateCompensationUseCase;
        this.markPendingCreateUseCase = markPendingCreateUseCase;
        this.buildPatientSnapshotUseCase = buildPatientSnapshotUseCase;
        this.nucleoPatientPort = nucleoPatientPort;
        this.pipelineExceptionUseCase = pipelineExceptionUseCase;
    }

    public SuccessResponseDTO register(HttpEnvelopeDTO<InboundAcolhimentoDTO> envelope) {
        InboundContextDTO<InboundAcolhimentoDTO> context = inboundContextMapper.fromEnvelop(envelope);
        UUID correlationId = context.envelop().correlationId();
        String correlationIdText = context.correlationIdText();

        InboundAcolhimentoDTO payload = inboundAcolhimentoMapper.toCreatePayload(context.payload());
        UUID patientId = payload.patientId();
        InboundAcolhimentoMapResultDTO mappingResult = inboundAcolhimentoMapper.mapCreate(payload, correlationIdText);

        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                correlationId,
                patientId,
                OperationType.CREATE,
                EMPTY_SNAPSHOT);

        if (nucleoPatientPort.existsByPatientId(patientId)) {
            markPendingCreateUseCase.markErrorQuietly(pending.getEventId());
            throw duplicatePatientException(correlationIdText, patientId);
        }

        try {
            createNucleoPatientUseCase.execute(mappingResult.nucleoPatients());
            createNucleoPatientResponsavelUseCase.execute(mappingResult.nucleoPatientResponsaveis());
        } catch (Exception ex) {
            markPendingCreateUseCase.markErrorQuietly(pending.getEventId());
            compensateLocalState(mappingResult);
            if (isDuplicateException(ex)) {
                throw duplicatePatientException(correlationIdText, patientId);
            }
            throw unwrap(ex, correlationIdText);
        }

        createPendingAcolhimentoUseCase.updateSnapshot(
                pending.getEventId(),
                buildPatientSnapshotUseCase.serialize(patientId, correlationIdText));

        try {
            outboxCommandUseCase.execute(
                    context.envelop(),
                    pending.getEventId(),
                    mappingResult,
                    RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        } catch (Exception ex) {
            markPendingCreateUseCase.markErrorQuietly(pending.getEventId());
            throw unwrap(ex, correlationIdText);
        }

        return new SuccessResponseDTO(
                "ACOLHIMENTO_CREATED",
                correlationIdText,
                "Acolhimento criado com sucesso",
                patientId);
    }

    private void compensateLocalState(InboundAcolhimentoMapResultDTO mappingResult) {
        java.util.List<UUID> nucleoPatientIds = mappingResult.nucleoPatients().stream()
                .map(nucleoPatient -> nucleoPatient != null ? nucleoPatient.getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        java.util.List<UUID> nucleoPatientResponsavelIds = mappingResult.nucleoPatientResponsaveis().stream()
                .map(responsavel -> responsavel != null ? responsavel.getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        try {
            deleteCreateCompensationUseCase.execute(nucleoPatientIds, nucleoPatientResponsavelIds);
        } catch (Exception ex) {
            log.error("Falha na compensacao local de create. nucleoPatientIds={}, responsavelIds={}",
                    nucleoPatientIds, nucleoPatientResponsavelIds, ex);
        }
    }

    private AcolhimentoException unwrap(Exception ex, String correlationId) {
        Throwable root = ex;
        if (ex instanceof CompletionException completionException && completionException.getCause() != null) {
            root = completionException.getCause();
        }

        if (isDuplicateException(root)) {
            return new AcolhimentoException(
                    ReasonCode.DUPLICATE_PATIENT,
                    correlationId,
                    "Acolhimento ja existe para o patientId informado");
        }

        Exception rootException = root instanceof Exception rootEx ? rootEx : ex;
        return pipelineExceptionUseCase.toPersistenceFailure(
                rootException,
                correlationId,
                "Falha no pipeline de create");
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
                if (normalized.contains("uk_patient_nucleo")
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
}
