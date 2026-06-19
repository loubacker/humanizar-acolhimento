package com.humanizar.acolhimento.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.exception.PipelineExceptionUseCase;
import com.humanizar.acolhimento.application.usecase.snapshot.BuildPatientSnapshotUseCase;
import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@Service
public class AcolhimentoUpdateService {

    private static final String EMPTY_SNAPSHOT = "{}";

    private final InboundContextMapper inboundContextMapper;
    private final InboundAcolhimentoMapper inboundAcolhimentoMapper;
    private final RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase;
    private final CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;
    private final DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase;
    private final DeleteNucleoPatientUseCase deleteNucleoPatientUseCase;
    private final CreateNucleoPatientUseCase createNucleoPatientUseCase;
    private final CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase;
    private final OutboxCommandUseCase outboxCommandUseCase;
    private final MarkPendingCreateUseCase markPendingCreateUseCase;
    private final BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;
    private final PipelineExceptionUseCase pipelineExceptionUseCase;

    public AcolhimentoUpdateService(
            InboundContextMapper inboundContextMapper,
            InboundAcolhimentoMapper inboundAcolhimentoMapper,
            RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase,
            CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase,
            DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase,
            DeleteNucleoPatientUseCase deleteNucleoPatientUseCase,
            CreateNucleoPatientUseCase createNucleoPatientUseCase,
            CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase,
            OutboxCommandUseCase outboxCommandUseCase,
            MarkPendingCreateUseCase markPendingCreateUseCase,
            BuildPatientSnapshotUseCase buildPatientSnapshotUseCase,
            PipelineExceptionUseCase pipelineExceptionUseCase) {
        this.inboundContextMapper = inboundContextMapper;
        this.inboundAcolhimentoMapper = inboundAcolhimentoMapper;
        this.retrieveNucleoPatientUseCase = retrieveNucleoPatientUseCase;
        this.createPendingAcolhimentoUseCase = createPendingAcolhimentoUseCase;
        this.deleteNucleoPatientResponsavelUseCase = deleteNucleoPatientResponsavelUseCase;
        this.deleteNucleoPatientUseCase = deleteNucleoPatientUseCase;
        this.createNucleoPatientUseCase = createNucleoPatientUseCase;
        this.createNucleoPatientResponsavelUseCase = createNucleoPatientResponsavelUseCase;
        this.outboxCommandUseCase = outboxCommandUseCase;
        this.markPendingCreateUseCase = markPendingCreateUseCase;
        this.buildPatientSnapshotUseCase = buildPatientSnapshotUseCase;
        this.pipelineExceptionUseCase = pipelineExceptionUseCase;
    }

    public SuccessResponseDTO updateByPatientId(
            UUID pathPatientId,
            HttpEnvelopeDTO<InboundAcolhimentoDTO> envelope) {
        InboundContextDTO<InboundAcolhimentoDTO> context = inboundContextMapper.fromUpdate(pathPatientId, envelope);
        UUID correlationId = context.envelop().correlationId();
        String correlationIdText = context.correlationIdText();

        InboundAcolhimentoDTO payload = inboundAcolhimentoMapper.toUpdatePayload(context.payload());
        UUID patientId = payload.patientId();

        List<NucleoPatient> existingNucleoPatients = retrieveNucleoPatientUseCase.execute(patientId);
        Map<UUID, UUID> existingIdsByNucleoId = toExistingIdsByNucleoId(existingNucleoPatients);
        Map<UUID, AnamneseStatus> existingStatusByNucleoId = toExistingStatusByNucleoId(existingNucleoPatients);

        InboundAcolhimentoMapResultDTO mappingResult = inboundAcolhimentoMapper.mapUpdate(
                payload,
                existingIdsByNucleoId,
                existingStatusByNucleoId,
                correlationIdText);

        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                correlationId,
                patientId,
                OperationType.UPDATE,
                EMPTY_SNAPSHOT);

        try {
            deleteNucleoPatientResponsavelUseCase.execute(patientId);
            deleteNucleoPatientUseCase.execute(patientId);
            createNucleoPatientUseCase.execute(mappingResult.nucleoPatients());
            createNucleoPatientResponsavelUseCase.execute(mappingResult.nucleoPatientResponsaveis());
        } catch (Exception ex) {
            markPendingCreateUseCase.markErrorQuietly(pending.getEventId());
            throw pipelineExceptionUseCase.toPersistenceFailure(
                    ex, correlationIdText, "Falha no pipeline de update");
        }

        createPendingAcolhimentoUseCase.updateSnapshot(
                pending.getEventId(),
                buildPatientSnapshotUseCase.serialize(patientId, correlationIdText));

        try {
            outboxCommandUseCase.execute(
                    context.envelop(),
                    pending.getEventId(),
                    mappingResult,
                    RoutingKeyCatalog.COMMAND_ACOLHIMENTO_UPDATED_V1);
        } catch (Exception ex) {
            markPendingCreateUseCase.markErrorQuietly(pending.getEventId());
            throw pipelineExceptionUseCase.toPersistenceFailure(
                    ex, correlationIdText, "Falha no pipeline de update");
        }

        return new SuccessResponseDTO(
                "ACOLHIMENTO_UPDATED",
                correlationIdText,
                "Acolhimento atualizado com sucesso",
                patientId);
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

    private Map<UUID, AnamneseStatus> toExistingStatusByNucleoId(List<NucleoPatient> existingNucleoPatients) {
        Map<UUID, AnamneseStatus> statusByNucleoId = new LinkedHashMap<>();
        if (existingNucleoPatients == null) {
            return statusByNucleoId;
        }

        for (NucleoPatient nucleoPatient : existingNucleoPatients) {
            if (nucleoPatient == null || nucleoPatient.getNucleoId() == null) {
                continue;
            }
            AnamneseStatus status = nucleoPatient.getAnamneseStatus() != null
                    ? nucleoPatient.getAnamneseStatus()
                    : AnamneseStatus.PENDENTE;
            statusByNucleoId.putIfAbsent(nucleoPatient.getNucleoId(), status);
        }
        return statusByNucleoId;
    }
}
