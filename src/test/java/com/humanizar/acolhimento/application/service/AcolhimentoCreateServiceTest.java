package com.humanizar.acolhimento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
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
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoPatientDTO;
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
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@ExtendWith(MockitoExtension.class)
class AcolhimentoCreateServiceTest {

    @Mock
    private InboundContextMapper inboundContextMapper;

    @Mock
    private InboundAcolhimentoMapper inboundAcolhimentoMapper;

    @Mock
    private CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;

    @Mock
    private CreateNucleoPatientUseCase createNucleoPatientUseCase;

    @Mock
    private CreateNucleoPatientResponsavelUseCase createNucleoPatientResponsavelUseCase;

    @Mock
    private OutboxCommandUseCase outboxCommandUseCase;

    @Mock
    private DeleteCreateCompensationUseCase deleteCreateCompensationUseCase;

    @Mock
    private MarkPendingCreateUseCase markPendingCreateUseCase;

    @Mock
    private BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;

    @Mock
    private NucleoPatientPort nucleoPatientPort;

    private AcolhimentoCreateService service;

    @BeforeEach
    void setUp() {
        service = new AcolhimentoCreateService(
                inboundContextMapper,
                inboundAcolhimentoMapper,
                createPendingAcolhimentoUseCase,
                createNucleoPatientUseCase,
                createNucleoPatientResponsavelUseCase,
                outboxCommandUseCase,
                deleteCreateCompensationUseCase,
                markPendingCreateUseCase,
                buildPatientSnapshotUseCase,
                nucleoPatientPort,
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
                patientId, List.of(), List.of(), java.util.Map.of());

        when(inboundContextMapper.fromEnvelop(envelope)).thenReturn(context);
        when(inboundAcolhimentoMapper.toCreatePayload(payload)).thenReturn(payload);
        when(inboundAcolhimentoMapper.mapCreate(eq(payload), eq(correlationId.toString())))
                .thenReturn(mappingResult);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.CREATE, "{}"))
                .thenReturn(pending(eventId, patientId, correlationId));
        when(nucleoPatientPort.existsByPatientId(patientId)).thenReturn(false);
        when(buildPatientSnapshotUseCase.serialize(patientId, correlationId.toString()))
                .thenReturn(completeSnapshot);

        SuccessResponseDTO response = service.register(envelope);

        InOrder inOrder = inOrder(
                createNucleoPatientUseCase,
                createNucleoPatientResponsavelUseCase,
                buildPatientSnapshotUseCase,
                createPendingAcolhimentoUseCase,
                outboxCommandUseCase);
        inOrder.verify(createNucleoPatientUseCase).execute(mappingResult.nucleoPatients());
        inOrder.verify(createNucleoPatientResponsavelUseCase).execute(mappingResult.nucleoPatientResponsaveis());
        inOrder.verify(buildPatientSnapshotUseCase).serialize(patientId, correlationId.toString());
        inOrder.verify(createPendingAcolhimentoUseCase).updateSnapshot(eventId, completeSnapshot);
        inOrder.verify(outboxCommandUseCase).execute(
                envelope, eventId, mappingResult, RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);

        assertEquals("ACOLHIMENTO_CREATED", response.code());
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
                patientId, List.of(), List.of(), java.util.Map.of());

        when(inboundContextMapper.fromEnvelop(envelope)).thenReturn(context);
        when(inboundAcolhimentoMapper.toCreatePayload(payload)).thenReturn(payload);
        when(inboundAcolhimentoMapper.mapCreate(eq(payload), eq(correlationId.toString())))
                .thenReturn(mappingResult);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.CREATE, "{}"))
                .thenReturn(pending(eventId, patientId, correlationId));
        when(nucleoPatientPort.existsByPatientId(patientId)).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(createNucleoPatientUseCase).execute(mappingResult.nucleoPatients());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.register(envelope));

        verify(markPendingCreateUseCase).markErrorQuietly(eventId);
        verify(buildPatientSnapshotUseCase, org.mockito.Mockito.never())
                .serialize(any(UUID.class), any());
        verify(createPendingAcolhimentoUseCase, org.mockito.Mockito.never())
                .updateSnapshot(any(UUID.class), any());
    }

    private PendingAcolhimento pending(UUID eventId, UUID patientId, UUID correlationId) {
        return PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .patientId(patientId)
                .operationType(OperationType.CREATE)
                .status(Status.PENDING)
                .payloadSnapshot("{}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
