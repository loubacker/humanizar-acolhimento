package com.humanizar.acolhimento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.mapper.AnamneseMapper;
import com.humanizar.acolhimento.application.http.mapper.HttpEnvelopeMapper;
import com.humanizar.acolhimento.application.http.mapper.InboundFieldValidator;
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

@ExtendWith(MockitoExtension.class)
class AcolhimentoAnamneseServiceTest {

    @Mock
    private NucleoPatientPort nucleoPatientPort;

    @Mock
    private SaveAnamneseUseCase saveAnamneseUseCase;

    @Mock
    private RetrieveAnamneseUseCase retrieveAnamneseUseCase;

    @Mock
    private DeleteAnamneseUseCase deleteAnamneseUseCase;

    @Mock
    private BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;

    @Mock
    private CreatePendingAcolhimentoUseCase createPendingAcolhimentoUseCase;

    @Mock
    private CreateAnamneseAuditOutboxUseCase createAnamneseUseCase;

    @Captor
    private ArgumentCaptor<Acolhimento> acolhimentoCaptor;

    private AcolhimentoAnamneseService service;

    @BeforeEach
    void setUp() {
        service = new AcolhimentoAnamneseService(
                nucleoPatientPort,
                saveAnamneseUseCase,
                retrieveAnamneseUseCase,
                deleteAnamneseUseCase,
                new AnamneseMapper(new InboundFieldValidator()),
                new HttpEnvelopeMapper(new InboundFieldValidator()),
                buildPatientSnapshotUseCase,
                createPendingAcolhimentoUseCase,
                createAnamneseUseCase);
    }

    @Test
    void shouldCreateAnamneseWithPendingSuccessAndAuditOutbox() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String snapshot = "{\"patientId\":\"" + patientId + "\",\"nucleos\":[]}";
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(correlationId,
                new AnamneseDTO(LocalDate.of(2026, 3, 2), LocalTime.of(11, 35), "obs"));

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(saveAnamneseUseCase.existsByNucleoPatientId(nucleoPatientId)).thenReturn(false);
        when(saveAnamneseUseCase.execute(any(Acolhimento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(buildPatientSnapshotUseCase.serialize(patientId, correlationId.toString())).thenReturn(snapshot);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.CREATE, Status.SUCCESS, snapshot))
                .thenReturn(pending(eventId, patientId, correlationId, OperationType.CREATE));

        SuccessResponseDTO response = service.create(patientId, nucleoId, envelope);

        verify(saveAnamneseUseCase).execute(acolhimentoCaptor.capture());
        Acolhimento saved = acolhimentoCaptor.getValue();
        assertEquals(nucleoPatientId, saved.getNucleoPatientId());

        verify(createPendingAcolhimentoUseCase).execute(
                correlationId, patientId, OperationType.CREATE, Status.SUCCESS, snapshot);
        verify(createAnamneseUseCase).execute(
                eq(envelope),
                eq(eventId),
                eq(patientId),
                eq(RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_CREATED_V1),
                any(AnamneseDTO.class));

        assertEquals("ANAMNESE_CREATED", response.code());
        assertEquals(correlationId.toString(), response.correlationId());
        assertEquals(LocalDate.of(2026, 3, 2), ((AnamneseDTO) response.data()).dataAnamnese());
    }

    @Test
    void shouldFailCreateWithNucleoNotRegisteredWhenNucleoDoesNotExist() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(UUID.randomUUID(),
                new AnamneseDTO(LocalDate.now(), LocalTime.NOON, "obs"));

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId)).thenReturn(Optional.empty());

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.create(patientId, nucleoId, envelope));

        assertEquals(ReasonCode.NUCLEO_NOT_REGISTERED, ex.getReasonCode());
        verify(saveAnamneseUseCase, never()).execute(any(Acolhimento.class));
        verify(createAnamneseUseCase, never())
                .execute(any(), any(), any(), any(), any());
    }

    @Test
    void shouldFailCreateWithAnamneseAlreadyExistsWhenAnamnesePresent() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(UUID.randomUUID(),
                new AnamneseDTO(LocalDate.now(), LocalTime.NOON, "obs"));

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(saveAnamneseUseCase.existsByNucleoPatientId(nucleoPatientId)).thenReturn(true);

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.create(patientId, nucleoId, envelope));

        assertEquals(ReasonCode.ANAMNESE_ALREADY_EXISTS, ex.getReasonCode());
        verify(saveAnamneseUseCase, never()).execute(any(Acolhimento.class));
        verify(createAnamneseUseCase, never())
                .execute(any(), any(), any(), any(), any());
    }

    @Test
    void shouldUpdateAnamneseWithPendingSuccessAndAuditOutbox() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();
        UUID anamneseId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String snapshot = "{\"patientId\":\"" + patientId + "\",\"nucleos\":[]}";
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(correlationId,
                new AnamneseDTO(LocalDate.of(2026, 3, 10), LocalTime.of(9, 0), "nova obs"));

        Acolhimento existing = Acolhimento.builder()
                .id(anamneseId)
                .nucleoPatientId(nucleoPatientId)
                .dataAnamnese(LocalDate.of(2026, 3, 2))
                .horaAnamnese(LocalTime.of(11, 35))
                .observacoes("obs antiga")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(retrieveAnamneseUseCase.execute(nucleoPatientId)).thenReturn(Optional.of(existing));
        when(saveAnamneseUseCase.execute(any(Acolhimento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(buildPatientSnapshotUseCase.serialize(patientId, correlationId.toString())).thenReturn(snapshot);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.UPDATE, Status.SUCCESS, snapshot))
                .thenReturn(pending(eventId, patientId, correlationId, OperationType.UPDATE));

        SuccessResponseDTO response = service.update(patientId, nucleoId, envelope);

        verify(saveAnamneseUseCase).execute(acolhimentoCaptor.capture());
        Acolhimento saved = acolhimentoCaptor.getValue();
        assertEquals(anamneseId, saved.getId());
        assertEquals(createdAt, saved.getCreatedAt());

        verify(createPendingAcolhimentoUseCase).execute(
                correlationId, patientId, OperationType.UPDATE, Status.SUCCESS, snapshot);
        verify(createAnamneseUseCase).execute(
                eq(envelope),
                eq(eventId),
                eq(patientId),
                eq(RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_UPDATED_V1),
                any(AnamneseDTO.class));

        assertEquals("ANAMNESE_UPDATED", response.code());
        assertEquals("nova obs", ((AnamneseDTO) response.data()).observacoes());
    }

    @Test
    void shouldFailUpdateWithAnamneseNotFoundWhenMissing() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(UUID.randomUUID(),
                new AnamneseDTO(LocalDate.now(), LocalTime.NOON, "obs"));

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(retrieveAnamneseUseCase.execute(nucleoPatientId)).thenReturn(Optional.empty());

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.update(patientId, nucleoId, envelope));

        assertEquals(ReasonCode.ANAMNESE_NOT_FOUND, ex.getReasonCode());
        verify(saveAnamneseUseCase, never()).execute(any(Acolhimento.class));
        verify(createAnamneseUseCase, never())
                .execute(any(), any(), any(), any(), any());
    }

    @Test
    void shouldDeleteAnamneseWithPendingSuccessAndAuditOutboxCapturingPreviousState() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String snapshot = "{\"patientId\":\"" + patientId + "\",\"nucleos\":[]}";
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(correlationId, null);

        Acolhimento existing = Acolhimento.builder()
                .id(UUID.randomUUID())
                .nucleoPatientId(nucleoPatientId)
                .dataAnamnese(LocalDate.of(2026, 3, 2))
                .horaAnamnese(LocalTime.of(11, 35))
                .observacoes("obs")
                .build();

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(retrieveAnamneseUseCase.execute(nucleoPatientId)).thenReturn(Optional.of(existing));
        when(buildPatientSnapshotUseCase.serialize(patientId, correlationId.toString())).thenReturn(snapshot);
        when(createPendingAcolhimentoUseCase.execute(
                correlationId, patientId, OperationType.DELETE, Status.SUCCESS, snapshot))
                .thenReturn(pending(eventId, patientId, correlationId, OperationType.DELETE));

        ArgumentCaptor<AnamneseDTO> stateCaptor = ArgumentCaptor.forClass(AnamneseDTO.class);

        SuccessResponseDTO response = service.delete(patientId, nucleoId, envelope);

        verify(deleteAnamneseUseCase).execute(nucleoPatientId);
        verify(createPendingAcolhimentoUseCase).execute(
                correlationId, patientId, OperationType.DELETE, Status.SUCCESS, snapshot);
        verify(createAnamneseUseCase).execute(
                eq(envelope),
                eq(eventId),
                eq(patientId),
                eq(RoutingKeyCatalog.AUDIT_ACOLHIMENTO_ANAMNESE_DELETED_V1),
                stateCaptor.capture());

        assertEquals(LocalDate.of(2026, 3, 2), stateCaptor.getValue().dataAnamnese());
        assertEquals("ANAMNESE_DELETED", response.code());
        assertEquals(patientId, response.data());
    }

    @Test
    void shouldFailDeleteWithAnamneseNotFoundWhenMissing() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(UUID.randomUUID(), null);

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(retrieveAnamneseUseCase.execute(nucleoPatientId)).thenReturn(Optional.empty());

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.delete(patientId, nucleoId, envelope));

        assertEquals(ReasonCode.ANAMNESE_NOT_FOUND, ex.getReasonCode());
        verify(deleteAnamneseUseCase, never()).execute(any(UUID.class));
        verify(createAnamneseUseCase, never())
                .execute(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRetrieveAnamneseWhenExisting() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();

        Acolhimento existing = Acolhimento.builder()
                .id(UUID.randomUUID())
                .nucleoPatientId(nucleoPatientId)
                .dataAnamnese(LocalDate.of(2026, 3, 2))
                .horaAnamnese(LocalTime.of(11, 35))
                .observacoes("obs")
                .build();

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(retrieveAnamneseUseCase.execute(nucleoPatientId)).thenReturn(Optional.of(existing));

        AnamneseDTO response = service.retrieve(patientId, nucleoId);

        assertEquals(LocalDate.of(2026, 3, 2), response.dataAnamnese());
        assertEquals(LocalTime.of(11, 35), response.horaAnamnese());
        assertEquals("obs", response.observacoes());
    }

    @Test
    void shouldFailRetrieveWithAnamneseNotFoundWhenMissing() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(retrieveAnamneseUseCase.execute(nucleoPatientId)).thenReturn(Optional.empty());

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.retrieve(patientId, nucleoId));

        assertEquals(ReasonCode.ANAMNESE_NOT_FOUND, ex.getReasonCode());
    }

    @Test
    void shouldFailCreateWithRequiredFieldWhenDataAnamneseMissing() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        UUID nucleoPatientId = UUID.randomUUID();
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(UUID.randomUUID(),
                new AnamneseDTO(null, LocalTime.NOON, "obs"));

        when(nucleoPatientPort.findByPatientIdAndNucleoId(patientId, nucleoId))
                .thenReturn(Optional.of(nucleoPatient(nucleoPatientId, patientId, nucleoId)));
        when(saveAnamneseUseCase.existsByNucleoPatientId(nucleoPatientId)).thenReturn(false);

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.create(patientId, nucleoId, envelope));

        assertEquals(ReasonCode.INBOUND_REQUIRED_FIELD, ex.getReasonCode());
        verify(saveAnamneseUseCase, never()).execute(any(Acolhimento.class));
        verify(createAnamneseUseCase, never())
                .execute(any(), any(), any(), any(), any());
    }

    @Test
    void shouldFailWhenEnvelopeCorrelationIdMissing() {
        UUID patientId = UUID.randomUUID();
        UUID nucleoId = UUID.randomUUID();
        HttpEnvelopeDTO<AnamneseDTO> envelope = envelope(null,
                new AnamneseDTO(LocalDate.now(), LocalTime.NOON, "obs"));

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> service.create(patientId, nucleoId, envelope));

        assertEquals(ReasonCode.INBOUND_REQUIRED_FIELD, ex.getReasonCode());
        verify(nucleoPatientPort, never()).findByPatientIdAndNucleoId(any(UUID.class), any(UUID.class));
    }

    private HttpEnvelopeDTO<AnamneseDTO> envelope(UUID correlationId, AnamneseDTO payload) {
        return new HttpEnvelopeDTO<>(
                correlationId,
                "humanizar-admin-dashboard",
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                payload);
    }

    private PendingAcolhimento pending(UUID eventId, UUID patientId, UUID correlationId, OperationType operationType) {
        return PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .patientId(patientId)
                .operationType(operationType)
                .status(Status.SUCCESS)
                .payloadSnapshot("{}")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private NucleoPatient nucleoPatient(UUID id, UUID patientId, UUID nucleoId) {
        return NucleoPatient.builder()
                .id(id)
                .patientId(patientId)
                .nucleoId(nucleoId)
                .nucleoPatientResponsavel(java.util.List.of())
                .build();
    }
}
