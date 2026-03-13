package com.humanizar.acolhimento.infrastructure.messaging.inbound.idempotency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.port.ProcessedEventPort;

@ExtendWith(MockitoExtension.class)
class ProcessedEventGuardTest {

    @Mock
    private ProcessedEventPort processedEventPort;

    @Test
    void shouldThrowDuplicateEventWhenAlreadyProcessed() {
        ProcessedEventGuard guard = new ProcessedEventGuard(processedEventPort);
        String consumerName = "cmd-acolhimento-create-requested-usecase";
        UUID eventId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        when(processedEventPort.existsByConsumerNameAndEventId(consumerName, eventId)).thenReturn(true);

        AcolhimentoException ex = assertThrows(AcolhimentoException.class,
                () -> guard.ensureNotProcessed(consumerName, eventId, correlationId));

        assertEquals(ReasonCode.DUPLICATE_EVENT, ex.getReasonCode());
        assertEquals(correlationId, ex.getCorrelationId());
        verify(processedEventPort).existsByConsumerNameAndEventId(consumerName, eventId);
    }

    @Test
    void shouldAllowProcessingWhenEventIsNew() {
        ProcessedEventGuard guard = new ProcessedEventGuard(processedEventPort);
        String consumerName = "cmd-acolhimento-create-requested-usecase";
        UUID eventId = UUID.randomUUID();

        when(processedEventPort.existsByConsumerNameAndEventId(consumerName, eventId)).thenReturn(false);

        assertDoesNotThrow(() -> guard.ensureNotProcessed(consumerName, eventId, null));
        verify(processedEventPort).existsByConsumerNameAndEventId(consumerName, eventId);
    }
}
