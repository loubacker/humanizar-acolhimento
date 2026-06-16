package com.humanizar.acolhimento.infrastructure.controller.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.controller.dto.ErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class AcolhimentoExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoExceptionHandler.class);

    @ExceptionHandler(AcolhimentoException.class)
    public ResponseEntity<ErrorResponseDTO> handleAcolhimentoException(
            AcolhimentoException exception,
            HttpServletRequest request) {
        int status = exception.getStatusCode();
        ReasonCode reasonCode = exception.getReasonCode() != null
                ? exception.getReasonCode()
                : ReasonCode.VALIDATION_ERROR;
        logHandledException(status, reasonCode, request, exception, exception.getMessage());
        return buildResponse(status, reasonCode, exception.getMessage(), request, exception.getCorrelationId());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        ReasonCode reasonCode = ReasonCode.REQUEST_BODY_INVALID;
        logHandledException(reasonCode.httpStatus(), reasonCode, request, exception, exception.getMessage());
        return buildResponse(reasonCode.httpStatus(), reasonCode, reasonCode.message(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        ReasonCode reasonCode = ReasonCode.REQUEST_PARAMETER_INVALID;
        logHandledException(reasonCode.httpStatus(), reasonCode, request, exception, exception.getMessage());
        return buildResponse(reasonCode.httpStatus(), reasonCode, reasonCode.message(), request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        ReasonCode reasonCode = ReasonCode.REQUEST_PARAMETER_MISSING;
        logHandledException(reasonCode.httpStatus(), reasonCode, request, exception, exception.getMessage());
        return buildResponse(reasonCode.httpStatus(), reasonCode, reasonCode.message(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleException(
            Exception exception,
            HttpServletRequest request) {
        ReasonCode reasonCode = ReasonCode.UNEXPECTED_ERROR;
        logHandledException(reasonCode.httpStatus(), reasonCode, request, exception, exception.getMessage());
        return buildResponse(reasonCode.httpStatus(), reasonCode, reasonCode.message(), request, null);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(
            int status,
            ReasonCode reasonCode,
            String message,
            HttpServletRequest request,
            String correlationId) {
        HttpStatus httpStatus = resolveStatus(status);
        ErrorResponseDTO body = new ErrorResponseDTO(
                status,
                httpStatus.getReasonPhrase(),
                reasonCode.name(),
                message,
                request != null ? request.getRequestURI() : null);

        if (correlationId != null && !correlationId.isBlank()) {
            return ResponseEntity.status(httpStatus)
                    .header("X-Correlation-ID", correlationId)
                    .body(body);
        }
        return ResponseEntity.status(httpStatus).body(body);
    }

    private HttpStatus resolveStatus(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private void logHandledException(
            int status,
            ReasonCode reasonCode,
            HttpServletRequest request,
            Exception exception,
            String message) {
        String path = request != null ? request.getRequestURI() : null;
        if (status >= 500) {
            log.error("Falha tratada no acolhimento. reasonCode={}, status={}, path={}",
                    reasonCode.name(), status, path, exception);
            return;
        }
        log.warn("Falha tratada no acolhimento. reasonCode={}, status={}, path={}, message={}",
                reasonCode.name(), status, path, message);
    }
}
