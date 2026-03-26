package com.humanizar.acolhimento.application.usecase.callback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.application.catalog.TargetCatalog;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;

@ExtendWith(MockitoExtension.class)
class FinalizePendingAcolhimentoUseCaseTest {

    @Mock
    private PendingAcolhimentoPort pendingAcolhimentoPort;

    @Mock
    private PendingTargetStatusPort pendingTargetStatusPort;

    @InjectMocks
    private FinalizePendingAcolhimentoUseCase useCase;

    @Test
    void shouldMarkPendingAsSuccessWhenAllTargetsSucceed() {
        UUID eventId = UUID.randomUUID();
        PendingAcolhimento pending = pending(eventId, Status.PENDING);
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(pending));
        when(pendingTargetStatusPort.findByEventId(eventId))
                .thenReturn(List.of(new PendingTargetStatus(UUID.randomUUID(), eventId,
                        TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO, Status.SUCCESS)));

        useCase.execute(eventId);

        assertEquals(Status.SUCCESS, pending.getStatus());
        verify(pendingAcolhimentoPort).save(pending);
    }

    @Test
    void shouldMarkPendingAsErrorWhenAnyTargetFails() {
        UUID eventId = UUID.randomUUID();
        PendingAcolhimento pending = pending(eventId, Status.PENDING);
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(pending));
        when(pendingTargetStatusPort.findByEventId(eventId))
                .thenReturn(List.of(
                        new PendingTargetStatus(UUID.randomUUID(), eventId,
                                TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO, Status.SUCCESS),
                        new PendingTargetStatus(UUID.randomUUID(), eventId,
                                TargetCatalog.TARGET_PROGRAMA_ATENDIMENTO, Status.ERROR)));

        useCase.execute(eventId);

        assertEquals(Status.ERROR, pending.getStatus());
        verify(pendingAcolhimentoPort).save(pending);
    }

    @Test
    void shouldKeepPendingWhenTargetsAreStillPending() {
        UUID eventId = UUID.randomUUID();
        PendingAcolhimento pending = pending(eventId, Status.PENDING);
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.of(pending));
        when(pendingTargetStatusPort.findByEventId(eventId))
                .thenReturn(List.of(new PendingTargetStatus(UUID.randomUUID(), eventId,
                        TargetCatalog.TARGET_NUCLEO_RELACIONAMENTO, Status.PENDING)));

        useCase.execute(eventId);

        assertEquals(Status.PENDING, pending.getStatus());
        verify(pendingAcolhimentoPort, never()).save(pending);
    }

    @Test
    void shouldIgnoreWhenPendingAcolhimentoNotFound() {
        UUID eventId = UUID.randomUUID();
        when(pendingAcolhimentoPort.findByEventId(eventId)).thenReturn(Optional.empty());

        useCase.execute(eventId);

        verify(pendingTargetStatusPort, never()).findByEventId(eventId);
        verify(pendingAcolhimentoPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private PendingAcolhimento pending(UUID eventId, Status status) {
        return PendingAcolhimento.builder()
                .eventId(eventId)
                .correlationId(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .operationType(OperationType.CREATE)
                .status(status)
                .payloadSnapshot("{}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
