package com.humanizar.acolhimento.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.application.http.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.messaging.inbound.CallbackStatusInterpreter;
import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.mapper.InboundDeleteContextMapper;
import com.humanizar.acolhimento.application.usecase.central.FindPendingByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.central.FindTargetsByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteOutboxCommandUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteProgramaUseCase;
import com.humanizar.acolhimento.application.usecase.delete.ValidateDeleteProgressUseCase;
import com.humanizar.acolhimento.application.usecase.exception.PipelineExceptionUseCase;
import com.humanizar.acolhimento.application.usecase.snapshot.BuildPatientSnapshotUseCase;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@Service
public class AcolhimentoDeleteService {

    private final InboundDeleteContextMapper inboundDeleteContextMapper;
    private final CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;
    private final DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase;
    private final DeleteNucleoPatientUseCase deleteNucleoPatientUseCase;
    private final AcolhimentoPort acolhimentoPort;
    private final DeleteOutboxCommandUseCase deleteOutboxCommandUseCase;
    private final DeleteProgramaUseCase deleteProgramaUseCase;
    private final ValidateDeleteProgressUseCase validateDeleteProgressUseCase;
    private final MarkPendingCreateUseCase markPendingCreateUseCase;
    private final FindPendingByEventIdUseCase findPendingByEventIdUseCase;
    private final FindTargetsByEventIdUseCase findTargetsByEventIdUseCase;
    private final BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;
    private final PipelineExceptionUseCase pipelineExceptionUseCase;
    private final CallbackStatusInterpreter callbackStatusInterpreter;

    public AcolhimentoDeleteService(
            InboundDeleteContextMapper inboundDeleteContextMapper,
            CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase,
            DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase,
            DeleteNucleoPatientUseCase deleteNucleoPatientUseCase,
            AcolhimentoPort acolhimentoPort,
            DeleteOutboxCommandUseCase deleteOutboxCommandUseCase,
            DeleteProgramaUseCase deleteProgramaUseCase,
            ValidateDeleteProgressUseCase validateDeleteProgressUseCase,
            MarkPendingCreateUseCase markPendingCreateUseCase,
            FindPendingByEventIdUseCase findPendingByEventIdUseCase,
            FindTargetsByEventIdUseCase findTargetsByEventIdUseCase,
            BuildPatientSnapshotUseCase buildPatientSnapshotUseCase,
            PipelineExceptionUseCase pipelineExceptionUseCase,
            CallbackStatusInterpreter callbackStatusInterpreter) {
        this.inboundDeleteContextMapper = inboundDeleteContextMapper;
        this.createPendingAcolhimentoUseCase = createPendingAcolhimentoUseCase;
        this.deleteNucleoPatientResponsavelUseCase = deleteNucleoPatientResponsavelUseCase;
        this.deleteNucleoPatientUseCase = deleteNucleoPatientUseCase;
        this.acolhimentoPort = acolhimentoPort;
        this.deleteOutboxCommandUseCase = deleteOutboxCommandUseCase;
        this.deleteProgramaUseCase = deleteProgramaUseCase;
        this.validateDeleteProgressUseCase = validateDeleteProgressUseCase;
        this.markPendingCreateUseCase = markPendingCreateUseCase;
        this.findPendingByEventIdUseCase = findPendingByEventIdUseCase;
        this.findTargetsByEventIdUseCase = findTargetsByEventIdUseCase;
        this.buildPatientSnapshotUseCase = buildPatientSnapshotUseCase;
        this.pipelineExceptionUseCase = pipelineExceptionUseCase;
        this.callbackStatusInterpreter = callbackStatusInterpreter;
    }

    @Transactional
    public SuccessResponseDTO deleteByPatientId(
            UUID pathPatientId,
            HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop) {
        InboundContextDTO<AcolhimentoDeleteDTO> context = inboundDeleteContextMapper.fromDelete(pathPatientId, envelop);
        UUID patientId = context.payload().patientId();
        UUID correlationId = context.envelop().correlationId();
        String correlationIdText = context.correlationIdText();

        validateDeleteProgressUseCase.execute(patientId, correlationIdText);

        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                correlationId,
                patientId,
                OperationType.DELETE,
                buildPatientSnapshotUseCase.serialize(patientId, correlationIdText));

        try {
            deleteOutboxCommandUseCase.execute(
                    context.envelop(),
                    pending.getEventId(),
                    patientId);
        } catch (Exception ex) {
            markPendingCreateUseCase.markErrorQuietly(pending.getEventId());
            throw pipelineExceptionUseCase.toPersistenceFailure(
                    ex, correlationIdText, "Falha no pipeline de delete");
        }

        return new SuccessResponseDTO(
                "ACOLHIMENTO_DELETED",
                correlationIdText,
                "Acolhimento removido com sucesso",
                patientId);
    }

    public void handlePostCallbackSaga(UUID eventId, String completedTarget, String callbackStatus) {
        if (!callbackStatusInterpreter.isProcessed(callbackStatus)) {
            return;
        }

        PendingAcolhimento pending = findPendingByEventIdUseCase.execute(eventId).orElse(null);
        if (pending == null || pending.getOperationType() != OperationType.DELETE) {
            return;
        }

        if (TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO.equals(completedTarget)) {
            findTargetsByEventIdUseCase.execute(eventId).stream()
                    .filter(t -> TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO.equals(t.getTargetService()))
                    .filter(t -> t.getStatus() == Status.ON_HOLD)
                    .findFirst()
                    .ifPresent(t -> deleteProgramaUseCase.execute(pending));
        }

        PendingAcolhimento updated = findPendingByEventIdUseCase.execute(eventId).orElse(null);
        if (updated != null && updated.getStatus() == Status.SUCCESS) {
            try {
                executeLocalDelete(updated.getPatientId());
            } catch (Exception ex) {
                markPendingCreateUseCase.markErrorQuietly(eventId);
                throw pipelineExceptionUseCase.toPersistenceFailure(
                        ex,
                        updated.getCorrelationId() != null ? updated.getCorrelationId().toString() : null,
                        "Falha no pipeline de delete");
            }
        }
    }

    private void executeLocalDelete(UUID patientId) {
        acolhimentoPort.deleteByPatientId(patientId);
        deleteNucleoPatientResponsavelUseCase.execute(patientId);
        deleteNucleoPatientUseCase.execute(patientId);
    }
}
