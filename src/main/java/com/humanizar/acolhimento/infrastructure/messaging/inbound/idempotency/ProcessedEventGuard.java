package com.humanizar.acolhimento.infrastructure.messaging.inbound.idempotency;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.port.ProcessedEventPort;

@Component
public class ProcessedEventGuard {

    private final ProcessedEventPort processedEventPort;

    public ProcessedEventGuard(ProcessedEventPort processedEventPort) {
        this.processedEventPort = processedEventPort;
    }

    public void ensureNotProcessed(String consumerName, UUID eventId, String correlationId) {
        if (processedEventPort.existsByConsumerNameAndEventId(consumerName, eventId)) {
            throw new AcolhimentoException(ReasonCode.DUPLICATE_EVENT, correlationId);
        }
    }
}
