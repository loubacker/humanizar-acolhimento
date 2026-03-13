package com.humanizar.acolhimento.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class AcolhimentoExceptionTest {

    @Test
    void shouldResolveMessageFromReasonCode() {
        AcolhimentoException exception = new AcolhimentoException(ReasonCode.VALIDATION_ERROR, "corr-1");

        assertEquals(ReasonCode.VALIDATION_ERROR.getMessage(), exception.getMessage());
        assertEquals("corr-1", exception.getCorrelationId());
        assertEquals(400, exception.getStatusCode());
        assertFalse(exception.isRetryable());
    }

    @Test
    void shouldPreserveExplicitMessage() {
        AcolhimentoException exception = new AcolhimentoException(
                ReasonCode.PERSISTENCE_FAILURE,
                "corr-2",
                "falha customizada");

        assertEquals("falha customizada", exception.getMessage());
        assertTrue(exception.isRetryable());
    }
}
