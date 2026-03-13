package com.humanizar.acolhimento.domain.exception;

import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

public class AcolhimentoException extends RuntimeException {

    private final ReasonCode reasonCode;
    private final String message;
    private final String correlationId;

    public AcolhimentoException(ReasonCode reasonCode,
            String correlationId,
            String message) {
        super(resolveMessage(reasonCode, message));
        this.reasonCode = reasonCode;
        this.message = resolveMessage(reasonCode, message);
        this.correlationId = correlationId;
    }

    public AcolhimentoException(ReasonCode reasonCode,
            String correlationId) {
        this(reasonCode, correlationId, null);
    }

    public int getStatusCode() {
        return reasonCode != null ? reasonCode.getStatusCode() : 500;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isRetryable() {
        return reasonCode != null && reasonCode.isRetryable();
    }

    private static String resolveMessage(ReasonCode reasonCode, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        if (reasonCode != null) {
            return reasonCode.getMessage();
        }
        return null;
    }
}
