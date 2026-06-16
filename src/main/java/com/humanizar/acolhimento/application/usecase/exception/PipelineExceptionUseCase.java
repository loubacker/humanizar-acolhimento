package com.humanizar.acolhimento.application.usecase.exception;

import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Service
public class PipelineExceptionUseCase {

    public AcolhimentoException toPersistenceFailure(Exception ex, String correlationId, String fallbackMessage) {
        if (ex instanceof AcolhimentoException acolhimentoException) {
            return acolhimentoException;
        }
        String message = ex != null && ex.getMessage() != null
                ? ex.getMessage()
                : fallbackMessage;
        return new AcolhimentoException(ReasonCode.PERSISTENCE_FAILURE, correlationId, message);
    }
}
