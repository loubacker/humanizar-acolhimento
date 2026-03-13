package com.humanizar.acolhimento.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.inbound.dto.InboundDeleteContextDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.inbound.mapper.InboundDeleteContextMapper;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteOutboxCommandUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
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
    private final MarkPendingCreateUseCase markPendingCreateUseCase;
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
            MarkPendingCreateUseCase markPendingCreateUseCase,
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
        this.markPendingCreateUseCase = markPendingCreateUseCase;
        this.objectMapper = objectMapper;
    }

    public void deleteByPatientId(
            UUID pathPatientId,
            InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop) {
        InboundDeleteContextDTO context = inboundDeleteContextMapper.fromDelete(pathPatientId, envelop);
        UUID patientId = context.payload().patientId();
        UUID correlationId = context.envelop().correlationId();
        String correlationIdText = correlationId != null ? correlationId.toString() : null;

        Acolhimento acolhimento = findAcolhimentoByPatientIdRetrieveUseCase.execute(patientId, correlationIdText);
        InboundAcolhimentoDTO snapshot = buildSnapshot(acolhimento, patientId);

        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                correlationId,
                patientId,
                OperationType.DELETE,
                serializeSnapshot(snapshot, correlationIdText));

        try {
            deleteNucleoPatientResponsavelUseCase.execute(patientId);
            deleteNucleoPatientUseCase.execute(patientId);
            deleteAcolhimentoUseCase.execute(patientId);
            deleteOutboxCommandUseCase.execute(
                    context.envelop(),
                    pending.getEventId(),
                    acolhimento.getId());
        } catch (Exception ex) {
            safeMarkPendingAsError(pending.getEventId());
            throw unwrap(ex, correlationIdText);
        }
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
