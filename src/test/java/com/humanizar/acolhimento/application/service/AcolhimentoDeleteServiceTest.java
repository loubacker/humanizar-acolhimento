package com.humanizar.acolhimento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.application.http.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.mapper.InboundDeleteContextMapper;
import com.humanizar.acolhimento.application.usecase.central.FindPendingByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.central.FindTargetsByEventIdUseCase;
import com.humanizar.acolhimento.application.usecase.create.CreatePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.create.MarkPendingCreateUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteNucleoPatientUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteOutboxCommandUseCase;
import com.humanizar.acolhimento.application.usecase.delete.DeleteProgramaUseCase;
import com.humanizar.acolhimento.application.usecase.delete.ValidateDeleteProgressUseCase;
import com.humanizar.acolhimento.application.messaging.inbound.CallbackStatusInterpreter;
import com.humanizar.acolhimento.application.usecase.exception.PipelineExceptionUseCase;
import com.humanizar.acolhimento.application.usecase.snapshot.BuildPatientSnapshotUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;

@ExtendWith(MockitoExtension.class)
class AcolhimentoDeleteServiceTest {

    @Mock
    private InboundDeleteContextMapper inboundDeleteContextMapper;

    @Mock
    private CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;

    @Mock
    private DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase;

    @Mock
    private DeleteNucleoPatientUseCase deleteNucleoPatientUseCase;

    @Mock
    private AcolhimentoPort acolhimentoPort;

    @Mock
    private DeleteOutboxCommandUseCase deleteOutboxCommandUseCase;

    @Mock
    private DeleteProgramaUseCase deleteProgramaUseCase;

    @Mock
    private ValidateDeleteProgressUseCase validateDeleteProgressUseCase;

    @Mock
    private MarkPendingCreateUseCase markPendingCreateUseCase;

    @Mock
    private FindPendingByEventIdUseCase findPendingByEventIdUseCase;

    @Mock
    private FindTargetsByEventIdUseCase findTargetsByEventIdUseCase;

    @Mock
    private BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;

    private AcolhimentoDeleteService service;

    @BeforeEach
    void setUp() {
        service = new AcolhimentoDeleteService(
                inboundDeleteContextMapper,
                createPendingAcolhimentoUseCase,
                deleteNucleoPatientResponsavelUseCase,
                deleteNucleoPatientUseCase,
                acolhimentoPort,
                deleteOutboxCommandUseCase,
                deleteProgramaUseCase,
                validateDeleteProgressUseCase,
                markPendingCreateUseCase,
                findPendingByEventIdUseCase,
                findTargetsByEventIdUseCase,
                buildPatientSnapshotUseCase,
                new PipelineExceptionUseCase(),
                new CallbackStatusInterpreter());
    }

    @Test
    void shouldBlockDeleteWhenThereIsPendingDeleteInProgress() {
        UUID pathPatientId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        AcolhimentoDeleteDTO payload = new AcolhimentoDeleteDTO(pathPatientId);
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
                correlationId,
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                payload);
        InboundContextDTO<AcolhimentoDeleteDTO> context = new InboundContextDTO<>(envelop, payload);

        when(inboundDeleteContextMapper.fromDelete(pathPatientId, envelop)).thenReturn(context);
        doThrow(new AcolhimentoException(
                ReasonCode.DELETE_IN_PROGRESS,
                correlationId.toString(),
                "Ja existe operacao DELETE pendente para patientId=" + pathPatientId))
                .when(validateDeleteProgressUseCase)
                .execute(pathPatientId, correlationId.toString());

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.deleteByPatientId(pathPatientId, envelop));

        assertEquals(ReasonCode.DELETE_IN_PROGRESS, ex.getReasonCode());
        verify(validateDeleteProgressUseCase).execute(pathPatientId, correlationId.toString());
        verify(createPendingAcolhimentoUseCase, never())
                .execute(
                        org.mockito.ArgumentMatchers.any(UUID.class),
                        org.mockito.ArgumentMatchers.any(UUID.class),
                        org.mockito.ArgumentMatchers.any(OperationType.class),
                        org.mockito.ArgumentMatchers.anyString());
        verify(deleteOutboxCommandUseCase, never())
                .execute(
                        org.mockito.ArgumentMatchers.<HttpEnvelopeDTO<AcolhimentoDeleteDTO>>any(),
                        org.mockito.ArgumentMatchers.any(UUID.class),
                        org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void shouldCreatePendingWithCompleteSnapshotBuiltBeforeRemoval() {
        UUID pathPatientId = UUID.randomUUID();
        UUID patientId = pathPatientId;
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String completeSnapshot = "{\"patientId\":\"" + patientId + "\",\"nucleos\":[]}";

        AcolhimentoDeleteDTO payload = new AcolhimentoDeleteDTO(pathPatientId);
        HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelop = new HttpEnvelopeDTO<>(
                correlationId,
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                payload);
        InboundContextDTO<AcolhimentoDeleteDTO> context = new InboundContextDTO<>(envelop, payload);

        when(inboundDeleteContextMapper.fromDelete(pathPatientId, envelop)).thenReturn(context);
        when(buildPatientSnapshotUseCase.serialize(patientId, correlationId.toString()))
                .thenReturn(completeSnapshot);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.DELETE, completeSnapshot))
                .thenReturn(pending(eventId, patientId, Status.PENDING));

        service.deleteByPatientId(pathPatientId, envelop);

        verify(buildPatientSnapshotUseCase).serialize(patientId, correlationId.toString());
        verify(createPendingAcolhimentoUseCase)
                .execute(correlationId, patientId, OperationType.DELETE, completeSnapshot);
        verify(deleteOutboxCommandUseCase).execute(envelop, eventId, patientId);
    }

    @Test
    void shouldTriggerProgramaDeleteWhenNucleoProcessedAndProgramaIsOnHold() {
        UUID eventId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        PendingAcolhimento pendingDelete = pending(eventId, patientId, Status.PENDING);
        PendingTargetStatus programaOnHold = new PendingTargetStatus(
                UUID.randomUUID(),
                eventId,
                TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO,
                Status.ON_HOLD);

        when(findPendingByEventIdUseCase.execute(eventId))
                .thenReturn(Optional.of(pendingDelete))
                .thenReturn(Optional.of(pendingDelete));
        when(findTargetsByEventIdUseCase.execute(eventId))
                .thenReturn(List.of(programaOnHold));

        service.handlePostCallbackSaga(eventId, TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO, "PROCESSED");

        verify(deleteProgramaUseCase).execute(pendingDelete);
        verify(deleteNucleoPatientResponsavelUseCase, never()).execute(patientId);
        verify(deleteNucleoPatientUseCase, never()).execute(patientId);
        verify(acolhimentoPort, never()).deleteByPatientId(patientId);
    }

    @Test
    void shouldExecuteLocalDeleteWhenProgramProcessedAndPendingBecomesSuccess() {
        UUID eventId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        PendingAcolhimento firstRead = pending(eventId, patientId, Status.PENDING);
        PendingAcolhimento afterFinalize = pending(eventId, patientId, Status.SUCCESS);

        when(findPendingByEventIdUseCase.execute(eventId))
                .thenReturn(Optional.of(firstRead))
                .thenReturn(Optional.of(afterFinalize));

        service.handlePostCallbackSaga(eventId, TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO, "PROCESSED");

        verify(acolhimentoPort).deleteByPatientId(patientId);
        verify(deleteNucleoPatientResponsavelUseCase).execute(patientId);
        verify(deleteNucleoPatientUseCase).execute(patientId);
        verify(deleteProgramaUseCase, never()).execute(org.mockito.ArgumentMatchers.<PendingAcolhimento>any());
    }

    private PendingAcolhimento pending(UUID eventId, UUID patientId, Status status) {
        return PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(UUID.randomUUID())
                .patientId(patientId)
                .operationType(OperationType.DELETE)
                .status(status)
                .payloadSnapshot("{}")
                .build();
    }
}
