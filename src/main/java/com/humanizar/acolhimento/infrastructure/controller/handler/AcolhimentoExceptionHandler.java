package com.humanizar.acolhimento.infrastructure.controller.handler;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class AcolhimentoExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoExceptionHandler.class);

    @ExceptionHandler(AcolhimentoException.class)
    public ResponseEntity<AcolhimentoErrorResponseDTO> handleAcolhimentoException(
            AcolhimentoException exception,
            HttpServletRequest request) {
        int status = exception.getStatusCode();
        String reasonCode = exception.getReasonCode() != null
                ? exception.getReasonCode().name()
                : ReasonCode.VALIDATION_ERROR.name();
        String path = request != null ? request.getRequestURI() : null;
        String correlationId = exception.getCorrelationId();

        log.error(
                "Erro no processamento HTTP. reasonCode={}, status={}, correlationId={}, path={}, causa={}",
                reasonCode,
                status,
                correlationId,
                path,
                rootCauseMessage(exception),
                exception);

        AcolhimentoErrorResponseDTO body = new AcolhimentoErrorResponseDTO(
                status,
                reasonCode,
                exception.getMessage(),
                correlationId,
                path,
                OffsetDateTime.now());

        return ResponseEntity.status(resolveStatus(status)).body(body);
    }

    private HttpStatus resolveStatus(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current != null ? current.getMessage() : null;
    }
}
