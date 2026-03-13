package com.humanizar.acolhimento.application.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.update.UpdateOutboxCommandUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoUpdateResponseDTO;

@Service
public class AcolhimentoUpdateService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoUpdateService.class);

    private final InboundContextMapper inboundContextMapper;
    private final InboundAcolhimentoMapper inboundAcolhimentoMapper;
    private final RetrieveAcolhimentoUseCase retrieveAcolhimentoUseCase;
    private final RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase;
    private final CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;
    private final CreateAcolhimentoUseCase createAcolhimentoUseCase;
    private final DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase;
    private final DeleteNucleoPatientUseCase deleteNucleoPatientUseCase;
    private final CreateNucleoPatientUseCase createNucleoPatientUseCase;
    private final CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase;
    private final UpdateOutboxCommandUseCase updateOutboxCommandUseCase;
    private final MarkPendingCreateUseCase markPendingCreateUseCase;
    private final ObjectMapper objectMapper;

    public AcolhimentoUpdateService(
            InboundContextMapper inboundContextMapper,
            InboundAcolhimentoMapper inboundAcolhimentoMapper,
            RetrieveAcolhimentoUseCase retrieveAcolhimentoUseCase,
            RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase,
            CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase,
            CreateAcolhimentoUseCase createAcolhimentoUseCase,
            DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase,
            DeleteNucleoPatientUseCase deleteNucleoPatientUseCase,
            CreateNucleoPatientUseCase createNucleoPatientUseCase,
            CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase,
            UpdateOutboxCommandUseCase updateOutboxCommandUseCase,
            MarkPendingCreateUseCase markPendingCreateUseCase,
            ObjectMapper objectMapper) {
        this.inboundContextMapper = inboundContextMapper;
        this.inboundAcolhimentoMapper = inboundAcolhimentoMapper;
        this.retrieveAcolhimentoUseCase = retrieveAcolhimentoUseCase;
        this.retrieveNucleoPatientUseCase = retrieveNucleoPatientUseCase;
        this.createPendingAcolhimentoUseCase = createPendingAcolhimentoUseCase;
        this.createAcolhimentoUseCase = createAcolhimentoUseCase;
        this.deleteNucleoPatientResponsavelUseCase = deleteNucleoPatientResponsavelUseCase;
        this.deleteNucleoPatientUseCase = deleteNucleoPatientUseCase;
        this.createNucleoPatientUseCase = createNucleoPatientUseCase;
        this.createNucleoPatientResponsavelUseCase = createNucleoPatientResponsavelUseCase;
        this.updateOutboxCommandUseCase = updateOutboxCommandUseCase;
        this.markPendingCreateUseCase = markPendingCreateUseCase;
        this.objectMapper = objectMapper;
    }

    public AcolhimentoUpdateResponseDTO updateByPatientId(
            UUID pathPatientId,
            InboundEnvelopeDTO<InboundAcolhimentoDTO> envelope) {
        InboundContextDTO<InboundAcolhimentoDTO> context = inboundContextMapper.fromUpdate(pathPatientId, envelope);
        UUID correlationId = context.envelop().correlationId();
        String correlationIdText = correlationId != null ? correlationId.toString() : null;

        InboundAcolhimentoDTO payload = inboundAcolhimentoMapper.toUpdatePayload(context.payload());
        UUID patientId = payload.patientId();

        Acolhimento existingAcolhimento = retrieveAcolhimentoUseCase.execute(patientId, correlationIdText);
        List<NucleoPatient> existingNucleoPatients = retrieveNucleoPatientUseCase.execute(patientId);
        Map<UUID, UUID> existingIdsByNucleoId = toExistingIdsByNucleoId(existingNucleoPatients);

        InboundAcolhimentoMappingResult mappingResult = inboundAcolhimentoMapper.mapUpdate(
                payload,
                existingAcolhimento.getId(),
                existingIdsByNucleoId,
                correlationIdText);
        mappingResult.acolhimento().setCreatedAt(existingAcolhimento.getCreatedAt());
        mappingResult.acolhimento().setUpdatedAt(LocalDateTime.now());

        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                correlationId,
                patientId,
                OperationType.UPDATE,
                serializeSnapshot(payload, correlationIdText));

        try {
            createAcolhimentoUseCase.execute(mappingResult.acolhimento());
            deleteNucleoPatientResponsavelUseCase.execute(patientId);
            deleteNucleoPatientUseCase.execute(patientId);
            createNucleoPatientUseCase.execute(mappingResult.nucleoPatients());
            createNucleoPatientResponsavelUseCase.execute(mappingResult.nucleoPatientResponsaveis());
        } catch (Exception ex) {
            safeMarkPendingAsError(pending.getEventId());
            throw unwrap(ex, correlationIdText);
        }

        try {
            updateOutboxCommandUseCase.execute(context.envelop(), pending.getEventId(), mappingResult);
        } catch (Exception ex) {
            safeMarkPendingAsError(pending.getEventId());
            throw unwrap(ex, correlationIdText);
        }

        return new AcolhimentoUpdateResponseDTO(
                "Acolhimento Atualizado com Sucesso para Paciente " + patientId,
                patientId,
                correlationId);
    }

    private Map<UUID, UUID> toExistingIdsByNucleoId(List<NucleoPatient> existingNucleoPatients) {
        Map<UUID, UUID> idsByNucleoId = new LinkedHashMap<>();
        if (existingNucleoPatients == null) {
            return idsByNucleoId;
        }

        for (NucleoPatient nucleoPatient : existingNucleoPatients) {
            if (nucleoPatient == null || nucleoPatient.getNucleoId() == null || nucleoPatient.getId() == null) {
                continue;
            }
            idsByNucleoId.putIfAbsent(nucleoPatient.getNucleoId(), nucleoPatient.getId());
        }
        return idsByNucleoId;
    }

    private void safeMarkPendingAsError(UUID eventId) {
        try {
            markPendingCreateUseCase.markError(eventId);
        } catch (Exception ex) {
            log.error("Falha ao marcar pending_acolhimento como ERROR no UPDATE. eventId={}", eventId, ex);
        }
    }

    private AcolhimentoException unwrap(Exception ex, String correlationId) {
        if (ex instanceof AcolhimentoException acolhimentoException) {
            return acolhimentoException;
        }
        String message = ex != null && ex.getMessage() != null
                ? ex.getMessage()
                : "Falha no pipeline de update";
        return new AcolhimentoException(ReasonCode.PERSISTENCE_FAILURE, correlationId, message);
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
