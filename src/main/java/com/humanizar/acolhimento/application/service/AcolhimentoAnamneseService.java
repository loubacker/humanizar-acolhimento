package com.humanizar.acolhimento.application.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.mapper.AnamneseMapper;
import com.humanizar.acolhimento.application.http.mapper.HttpEnvelopeMapper;
import com.humanizar.acolhimento.application.usecase.anamnese.CreateAnamneseAuditOutboxUseCase;
import com.humanizar.acolhimento.application.usecase.anamnese.DeleteAnamneseUseCase;
import com.humanizar.acolhimento.application.usecase.anamnese.RetrieveAnamneseUseCase;
import com.humanizar.acolhimento.application.usecase.anamnese.SaveAnamneseUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.snapshot.BuildPatientSnapshotUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@Service
public class AcolhimentoAnamneseService {

    private final NucleoPatientPort nucleoPatientPort;
    private final SaveAnamneseUseCase saveAnamneseUseCase;
    private final RetrieveAnamneseUseCase retrieveAnamneseUseCase;
    private final DeleteAnamneseUseCase deleteAnamneseUseCase;
    private final AnamneseMapper anamneseMapper;
    private final HttpEnvelopeMapper httpEnvelopeMapper;
    private final BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;
    private final CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;
    private final CreateAnamneseAuditOutboxUseCase createAnamneseUseCase;

    public AcolhimentoAnamneseService(
            NucleoPatientPort nucleoPatientPort,
            SaveAnamneseUseCase saveAnamneseUseCase,
            RetrieveAnamneseUseCase retrieveAnamneseUseCase,
            DeleteAnamneseUseCase deleteAnamneseUseCase,
            AnamneseMapper anamneseMapper,
            HttpEnvelopeMapper httpEnvelopeMapper,
            BuildPatientSnapshotUseCase buildPatientSnapshotUseCase,
            CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase,
            CreateAnamneseAuditOutboxUseCase createAnamneseUseCase) {
        this.nucleoPatientPort = nucleoPatientPort;
        this.saveAnamneseUseCase = saveAnamneseUseCase;
        this.retrieveAnamneseUseCase = retrieveAnamneseUseCase;
        this.deleteAnamneseUseCase = deleteAnamneseUseCase;
        this.anamneseMapper = anamneseMapper;
        this.httpEnvelopeMapper = httpEnvelopeMapper;
        this.buildPatientSnapshotUseCase = buildPatientSnapshotUseCase;
        this.createPendingAcolhimentoUseCase = createPendingAcolhimentoUseCase;
        this.createAnamneseUseCase = createAnamneseUseCase;
    }

    public SuccessResponseDTO create(UUID patientId, UUID nucleoId, HttpEnvelopeDTO<AnamneseDTO> envelope) {
        String correlationId = httpEnvelopeMapper.validateTracing(envelope);
        UUID nucleoPatientId = resolveNucleoPatientId(patientId, nucleoId, correlationId);

        if (saveAnamneseUseCase.existsByNucleoPatientId(nucleoPatientId)) {
            throw anamneseAlreadyExists(nucleoId, correlationId);
        }

        LocalDateTime now = LocalDateTime.now();
        Acolhimento anamnese = anamneseMapper.toDomain(envelope.payload(), correlationId);
        anamnese.setNucleoPatientId(nucleoPatientId);
        anamnese.setCreatedAt(now);
        anamnese.setUpdatedAt(now);

        Acolhimento saved = saveAnamneseUseCase.execute(anamnese);

        AnamneseDTO state = anamneseMapper.toDTO(saved);
        registerAudit(
                envelope,
                patientId,
                correlationId,
                OperationType.CREATE,
                RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_CREATED_V1,
                state);

        return new SuccessResponseDTO(
                "ANAMNESE_CREATED",
                correlationId,
                "Anamnese criada com sucesso",
                state);
    }

    public SuccessResponseDTO update(UUID patientId, UUID nucleoId, HttpEnvelopeDTO<AnamneseDTO> envelope) {
        String correlationId = httpEnvelopeMapper.validateTracing(envelope);
        UUID nucleoPatientId = resolveNucleoPatientId(patientId, nucleoId, correlationId);

        Acolhimento existing = retrieveAnamneseUseCase.execute(nucleoPatientId)
                .orElseThrow(() -> anamneseNotFound(nucleoId, correlationId));

        Acolhimento updated = anamneseMapper.toDomain(envelope.payload(), correlationId);
        updated.setId(existing.getId());
        updated.setNucleoPatientId(nucleoPatientId);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(LocalDateTime.now());

        Acolhimento saved = saveAnamneseUseCase.execute(updated);

        AnamneseDTO state = anamneseMapper.toDTO(saved);
        registerAudit(
                envelope,
                patientId,
                correlationId,
                OperationType.UPDATE,
                RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_UPDATED_V1,
                state);

        return new SuccessResponseDTO(
                "ANAMNESE_UPDATED",
                correlationId,
                "Anamnese atualizada com sucesso",
                state);
    }

    public SuccessResponseDTO delete(UUID patientId, UUID nucleoId, HttpEnvelopeDTO<AnamneseDTO> envelope) {
        String correlationId = httpEnvelopeMapper.validateTracing(envelope);
        UUID nucleoPatientId = resolveNucleoPatientId(patientId, nucleoId, correlationId);

        Acolhimento existing = retrieveAnamneseUseCase.execute(nucleoPatientId)
                .orElseThrow(() -> anamneseNotFound(nucleoId, correlationId));

        AnamneseDTO previousState = anamneseMapper.toDTO(existing);

        deleteAnamneseUseCase.execute(nucleoPatientId);

        registerAudit(
                envelope,
                patientId,
                correlationId,
                OperationType.DELETE,
                RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_DELETED_V1,
                previousState);

        return new SuccessResponseDTO(
                "ANAMNESE_DELETED",
                correlationId,
                "Anamnese removida com sucesso",
                patientId);
    }

    public AnamneseDTO retrieve(UUID patientId, UUID nucleoId) {
        String correlationId = UUID.randomUUID().toString();
        UUID nucleoPatientId = resolveNucleoPatientId(patientId, nucleoId, correlationId);

        Acolhimento existing = retrieveAnamneseUseCase.execute(nucleoPatientId)
                .orElseThrow(() -> anamneseNotFound(nucleoId, correlationId));

        return anamneseMapper.toDTO(existing);
    }

    private void registerAudit(
            HttpEnvelopeDTO<AnamneseDTO> envelope,
            UUID patientId,
            String correlationId,
            OperationType operationType,
            String routingKey,
            AnamneseDTO state) {
        PendingAcolhimento pending = createPendingAcolhimentoUseCase.execute(
                UUID.fromString(correlationId),
                patientId,
                operationType,
                Status.SUCCESS,
                buildPatientSnapshotUseCase.serialize(patientId, correlationId));

        createAnamneseUseCase.execute(
                envelope,
                pending.getEventId(),
                patientId,
                routingKey,
                state);
    }

    private UUID resolveNucleoPatientId(UUID patientId, UUID nucleoId, String correlationId) {
        NucleoPatient nucleoPatient = nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId)
                .orElseThrow(() -> nucleoNotRegistered(patientId, nucleoId, correlationId));
        return nucleoPatient.getId();
    }

    private AcolhimentoException nucleoNotRegistered(UUID patientId, UUID nucleoId, String correlationId) {
        return new AcolhimentoException(
                ReasonCode.NUCLEO_NOT_REGISTERED,
                correlationId,
                "Nucleo nao cadastrado para patientId=" + patientId + ", nucleoId=" + nucleoId);
    }

    private AcolhimentoException anamneseAlreadyExists(UUID nucleoId, String correlationId) {
        return new AcolhimentoException(
                ReasonCode.ANAMNESE_ALREADY_EXISTS,
                correlationId,
                "Anamnese ja existe para nucleoId=" + nucleoId);
    }

    private AcolhimentoException anamneseNotFound(UUID nucleoId, String correlationId) {
        return new AcolhimentoException(
                ReasonCode.ANAMNESE_NOT_FOUND,
                correlationId,
                "Anamnese nao encontrada para nucleoId=" + nucleoId);
    }
}
