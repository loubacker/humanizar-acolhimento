package com.humanizar.acolhimento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.application.http.dto.InboundContextDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoMapResultDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.http.mapper.InboundAcolhimentoMapper;
import com.humanizar.acolhimento.application.http.mapper.InboundContextMapper;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
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
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@ExtendWith(MockitoExtension.class)
class AcolhimentoUpdateServiceTest {

    @Mock
    private InboundContextMapper inboundContextMapper;

    @Mock
    private InboundAcolhimentoMapper inboundAcolhimentoMapper;

    @Mock
    private RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase;

    @Mock
    private CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;

    @Mock
    private DeleteNucleoPatientResponsavelUseCase deleteNucleoPatientResponsavelUseCase;

    @Mock
    private DeleteNucleoPatientUseCase deleteNucleoPatientUseCase;

    @Mock
    private CreateNucleoPatientUseCase createNucleoPatientUseCase;

    @Mock
    private CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase;

    @Mock
    private OutboxCommandUseCase outboxCommandUseCase;

    @Mock
    private MarkPendingCreateUseCase markPendingCreateUseCase;

    @Mock
    private BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;

    private AcolhimentoUpdateService service;

    @BeforeEach
    void setUp() {
        service = new AcolhimentoUpdateService(
                inboundContextMapper,
                inboundAcolhimentoMapper,
                retrieveNucleoPatientUseCase,
                createPendingAcolhimentoUseCase,
                deleteNucleoPatientResponsavelUseCase,
                deleteNucleoPatientUseCase,
                createNucleoPatientUseCase,
                createNucleoPatientResponsavelUseCase,
                outboxCommandUseCase,
                markPendingCreateUseCase,
                buildPatientSnapshotUseCase,
                new PipelineExceptionUseCase());
    }

    @Test
    void shouldFinalizeSnapshotWithCompleteStateAfterPersistence() {
        UUID patientId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String completeSnapshot = "{\"patientId\":\"" + patientId + "\",\"nucleos\":[]}";

        InboundAcolhimentoDTO payload = new InboundAcolhimentoDTO(patientId, List.<NucleoPatientDTO>of());
        HttpEnvelopeDTO<InboundAcolhimentoDTO> envelope = new HttpEnvelopeDTO<>(
                correlationId,
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                payload);
        InboundContextDTO<InboundAcolhimentoDTO> context = new InboundContextDTO<>(envelope, payload);
        InboundAcolhimentoMapResultDTO mappingResult = new InboundAcolhimentoMapResultDTO(
                patientId, List.of(), List.of(), Map.of());

        when(inboundContextMapper.fromUpdate(patientId, envelope)).thenReturn(context);
        when(inboundAcolhimentoMapper.toUpdatePayload(payload)).thenReturn(payload);
        when(retrieveNucleoPatientUseCase.execute(patientId)).thenReturn(List.of());
        when(inboundAcolhimentoMapper.mapUpdate(eq(payload), any(), any(), eq(correlationId.toString())))
                .thenReturn(mappingResult);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.UPDATE, "{}"))
                .thenReturn(pending(eventId, patientId, correlationId));
        when(buildPatientSnapshotUseCase.serialize(patientId, correlationId.toString()))
                .thenReturn(completeSnapshot);

        SuccessResponseDTO response = service.updateByPatientId(patientId, envelope);

        InOrder inOrder = inOrder(
                deleteNucleoPatientResponsavelUseCase,
                deleteNucleoPatientUseCase,
                createNucleoPatientUseCase,
                createNucleoPatientResponsavelUseCase,
                buildPatientSnapshotUseCase,
                createPendingAcolhimentoUseCase,
                outboxCommandUseCase);
        inOrder.verify(deleteNucleoPatientResponsavelUseCase).execute(patientId);
        inOrder.verify(deleteNucleoPatientUseCase).execute(patientId);
        inOrder.verify(createNucleoPatientUseCase).execute(mappingResult.nucleoPatients());
        inOrder.verify(createNucleoPatientResponsavelUseCase).execute(mappingResult.nucleoPatientResponsaveis());
        inOrder.verify(buildPatientSnapshotUseCase).serialize(patientId, correlationId.toString());
        inOrder.verify(createPendingAcolhimentoUseCase).updateSnapshot(eventId, completeSnapshot);
        inOrder.verify(outboxCommandUseCase).execute(
                envelope, eventId, mappingResult, RoutingKeyCatalog.COMMAND_ACOLHIMENTO_UPDATED_V1);

        assertEquals("ACOLHIMENTO_UPDATED", response.code());
        assertEquals(patientId, response.data());
    }

    @Test
    void shouldNotFinalizeSnapshotWhenLocalPersistenceFails() {
        UUID patientId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        InboundAcolhimentoDTO payload = new InboundAcolhimentoDTO(patientId, List.<NucleoPatientDTO>of());
        HttpEnvelopeDTO<InboundAcolhimentoDTO> envelope = new HttpEnvelopeDTO<>(
                correlationId,
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                payload);
        InboundContextDTO<InboundAcolhimentoDTO> context = new InboundContextDTO<>(envelope, payload);
        InboundAcolhimentoMapResultDTO mappingResult = new InboundAcolhimentoMapResultDTO(
                patientId, List.of(), List.of(), Map.of());

        when(inboundContextMapper.fromUpdate(patientId, envelope)).thenReturn(context);
        when(inboundAcolhimentoMapper.toUpdatePayload(payload)).thenReturn(payload);
        when(retrieveNucleoPatientUseCase.execute(patientId)).thenReturn(List.of());
        when(inboundAcolhimentoMapper.mapUpdate(eq(payload), any(), any(), eq(correlationId.toString())))
                .thenReturn(mappingResult);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.UPDATE, "{}"))
                .thenReturn(pending(eventId, patientId, correlationId));
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(createNucleoPatientUseCase).execute(mappingResult.nucleoPatients());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.updateByPatientId(patientId, envelope));

        verify(markPendingCreateUseCase).markErrorQuietly(eventId);
        verify(buildPatientSnapshotUseCase, never()).serialize(any(UUID.class), any());
        verify(createPendingAcolhimentoUseCase, never()).updateSnapshot(any(UUID.class), any());
    }

    private PendingAcolhimento pending(UUID eventId, UUID patientId, UUID correlationId) {
        return PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .patientId(patientId)
                .operationType(OperationType.UPDATE)
                .status(Status.PENDING)
                .payloadSnapshot("{}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
