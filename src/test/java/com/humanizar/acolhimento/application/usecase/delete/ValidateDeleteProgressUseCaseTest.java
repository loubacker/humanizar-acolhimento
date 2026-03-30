package com.humanizar.acolhimento.application.usecase.delete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;

@ExtendWith(MockitoExtension.class)
class ValidateDeleteProgressUseCaseTest {

    @Mock
    private PendingAcolhimentoPort pendingAcolhimentoPort;

    private ValidateDeleteProgressUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ValidateDeleteProgressUseCase(pendingAcolhimentoPort);
    }

    @Test
    void shouldThrowWhenDeleteIsAlreadyPending() {
        UUID patientId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        when(pendingAcolhimentoPort.checkDeleteStatusByPatientId(
                patientId,
                OperationType.DELETE,
                Status.PENDING))
                .thenReturn(true);

        AcolhimentoException ex = assertThrows(
                AcolhimentoException.class,
                () -> useCase.execute(patientId, correlationId));

        assertEquals(ReasonCode.DELETE_IN_PROGRESS, ex.getReasonCode());
        verify(pendingAcolhimentoPort).checkDeleteStatusByPatientId(
                patientId,
                OperationType.DELETE,
                Status.PENDING);
    }

    @Test
    void shouldContinueWhenThereIsNoDeletePending() {
        UUID patientId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        when(pendingAcolhimentoPort.checkDeleteStatusByPatientId(
                patientId,
                OperationType.DELETE,
                Status.PENDING))
                .thenReturn(false);

        assertDoesNotThrow(() -> useCase.execute(patientId, correlationId));
        verify(pendingAcolhimentoPort).checkDeleteStatusByPatientId(
                patientId,
                OperationType.DELETE,
                Status.PENDING);
    }
}
