package com.humanizar.acolhimento.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.application.inbound.dto.InboundDeleteContextDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.inbound.mapper.InboundDeleteContextMapper;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.usecase.central.FindPendingByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.central.FindTargetsByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteOutboxCommandUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteProgramaUseCase;
import com.humanizar.acolhimento.application.usecase.delete.ValidateDeleteProgressUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;

@Service
public class AcolhimentoDeleteService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoDeleteService.class);

    private final RetrieveAcolhimentoUseCase findAcolhimentoByPatientIdRetrieveUseCase;
    private final RetrieveNucleoPatientUseCase findNucleoPatientByPatientIdRetrieveUseCase;
    private final RetrieveNucleoPatientResponsavelUseCase findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase;
    private final InboundDeleteContextMapper inboundDeleteContextMapper;
    private final CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;
    private final DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase;
    private final DeleteNucleoPatientUseCase deleteNucleoPatientUseCase;
    private final DeleteAcolhimentoUseCase deleteAcolhimentoUseCase;
    private final DeleteOutboxCommandUseCase deleteOutboxCommandUseCase;
    private final DeleteProgramaUseCase deleteProgramaUseCase;
    private final ValidateDeleteProgressUseCase validateDeleteProgressUseCase;
    private final MarkPendingCreateUseCase markPendingCreateUseCase;
    private final FindPendingByEventIdUseCase findPendingByEventIdUseCase;
    private final FindTargetsByEventIdUseCase findTargetsByEventIdUseCase;
    private final ObjectMapper objectMapper;

    public AcolhimentoDeleteService(
            RetrieveAcolhimentoUseCase findAcolhimentoByPatientIdRetrieveUseCase,
            RetrieveNucleoPatientUseCase findNucleoPatientByPatientIdRetrieveUseCase,
            RetrieveNucleoPatientResponsavelUseCase findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase,
            InboundDeleteContextMapper inboundDeleteContextMapper,
            CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase,
            DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase,
            DeleteNucleoPatientUseCase deleteNucleoPatientUseCase,
            DeleteAcolhimentoUseCase deleteAcolhimentoUseCase,
            DeleteOutboxCommandUseCase deleteOutboxCommandUseCase,
            DeleteProgramaUseCase deleteProgramaUseCase,
            ValidateDeleteProgressUseCase validateDeleteProgressUseCase,
            MarkPendingCreateUseCase markPendingCreateUseCase,
            FindPendingByEventIdUseCase findPendingByEventIdUseCase,
            FindTargetsByEventIdUseCase findTargetsByEventIdUseCase,
            ObjectMapper objectMapper) {
        this.findAcolhimentoByPatientIdRetrieveUseCase = findAcolhimentoByPatientIdRetrieveUseCase;
        this.findNucleoPatientByPatientIdRetrieveUseCase = findNucleoPatientByPatientIdRetrieveUseCase;
        this.findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase = findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase;
        this.inboundDeleteContextMapper = inboundDeleteContextMapper;
        this.createPendingAcolhimentoUseCase = createPendingAcolhimentoUseCase;
        this.deleteNucleoPatientResponsavelUseCase = deleteNucleoPatientResponsavelUseCase;
        this.deleteNucleoPatientUseCase = deleteNucleoPatientUseCase;
        this.deleteAcolhimentoUseCase = deleteAcolhimentoUseCase;
        this.deleteOutboxCommandUseCase = deleteOutboxCommandUseCase;
        this.deleteProgramaUseCase = deleteProgramaUseCase;
        this.validateDeleteProgressUseCase = validateDeleteProgressUseCase;
        this.markPendingCreateUseCase = markPendingCreateUseCase;
        this.findPendingByEventIdUseCase = findPendingByEventIdUseCase;
        this.findTargetsByEventIdUseCase = findTargetsByEventIdUseCase;
        this.objectMapper = objectMapper;
    }

    public void deleteByPatientId(
            UUID pathPatientId,
            InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop) {
        InboundDeleteContextDTO context = inboundDeleteContextMapper.fromDelete(pathPatientId, envelop);
        UUID patientId = context.payload().patientId();
        UUID correlationId = context.envelop().correlationId();
        String correlationIdText = correlationId != null ? correlationId.toString() : null;

        validateDeleteProgressUseCase.execute(patientId, correlationIdText);

        Acolhimento acolhimento = findAcolhimentoByPatientIdRetrieveUseCase.execute(patientId, correlationIdText);
        InboundAcolhimentoDTO snapshot = buildSnapshot(acolhimento, patientId);

        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                correlationId,
                patientId,
                OperationType.DELETE,
                serializeSnapshot(snapshot, correlationIdText));

        try {
            deleteOutboxCommandUseCase.execute(
                    context.envelop(),
                    pending.getEventId(),
                    acolhimento.getId());
        } catch (Exception ex) {
            safeMarkPendingAsError(pending.getEventId());
            throw unwrap(ex, correlationIdText);
        }
    }

    public void handlePostCallbackSaga(UUID eventId, String completedTarget, String callbackStatus) {
        if (!"PROCESSED".equalsIgnoreCase(callbackStatus)) {
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
                safeMarkPendingAsError(eventId);
                throw unwrap(ex,
                        updated.getCorrelationId() != null ? updated.getCorrelationId().toString() : null);
            }
        }
    }

    private void executeLocalDelete(UUID patientId) {
        deleteNucleoPatientResponsavelUseCase.execute(patientId);
        deleteNucleoPatientUseCase.execute(patientId);
        deleteAcolhimentoUseCase.execute(patientId);
    }

    private void safeMarkPendingAsError(UUID eventId) {
        try {
            markPendingCreateUseCase.markError(eventId);
        } catch (Exception ex) {
            log.error("Falha ao marcar pending_acolhimento como ERROR no DELETE. eventId={}", eventId, ex);
        }
    }

    private AcolhimentoException unwrap(Exception ex, String correlationId) {
        if (ex instanceof AcolhimentoException acolhimentoException) {
            return acolhimentoException;
        }
        String message = ex != null && ex.getMessage() != null
                ? ex.getMessage()
                : "Falha no pipeline de delete";
        return new AcolhimentoException(ReasonCode.PERSISTENCE_FAILURE, correlationId, message);
    }

    private InboundAcolhimentoDTO buildSnapshot(Acolhimento acolhimento, UUID patientId) {
        var nucleoPatients = findNucleoPatientByPatientIdRetrieveUseCase.execute(patientId);
        var nucleoPatientDTOs = nucleoPatients.stream()
                .map(nucleoPatient -> new NucleoPatientDTO(
                        nucleoPatient.getNucleoId(),
                        findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase.execute(nucleoPatient.getId())
                                .stream()
                                .map(responsavel -> new NucleoResponsavelDTO(
                                        responsavel.getResponsavelId(),
                                        responsavel.getRole() != null ? responsavel.getRole().name() : null))
                                .toList()))
                .toList();

        return new InboundAcolhimentoDTO(
                acolhimento.getPatientId(),
                acolhimento.getDataAnamnese(),
                acolhimento.getHoraAnamnese(),
                acolhimento.getObservacoes(),
                nucleoPatientDTOs);
    }

    private String serializeSnapshot(InboundAcolhimentoDTO snapshot, String correlationId) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new AcolhimentoException(
                    ReasonCode.PERSISTENCE_FAILURE,
                    correlationId,
                    "Falha ao serializar payloadSnapshot de pending_acolhimento para delete");
        }
    }
}
