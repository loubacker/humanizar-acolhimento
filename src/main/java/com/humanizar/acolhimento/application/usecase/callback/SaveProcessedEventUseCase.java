package com.humanizar.acolhimento.application.usecase.callback;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.application.outbound.CallbackDTO;
import com.humanizar.acolhimento.domain.model.ProcessedEvent;
import com.humanizar.acolhimento.domain.model.enums.ProcessedResult;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.port.ProcessedEventPort;

@Service
public class SaveProcessedEventUseCase {

    private final ProcessedEventPort processedEventPort;

    public SaveProcessedEventUseCase(ProcessedEventPort processedEventPort) {
        this.processedEventPort = processedEventPort;
    }

    @Transactional
    public void execute(String consumerName, CallbackDTO callback) {
        ProcessedEvent event = ProcessedEvent.builder()
                .consumerName(consumerName)
                .eventId(callback.eventId())
                .correlationId(callback.correlationId())
                .sourceExchange(callback.exchangeName())
                .sourceRoutingKey(callback.routingKey())
                .aggregateType(callback.aggregateType())
                .aggregateId(callback.aggregateId())
                .actorId(callback.actorId())
                .userAgent(callback.userAgent())
                .originIp(callback.originIp())
                .processedAt(LocalDateTime.now())
                .result(resolveResult(callback.status()))
                .reasonCode(resolveReasonCode(callback.reasonCode()))
                .errorMessage(callback.errorMessage())
                .build();
        processedEventPort.save(event);
    }

    private ProcessedResult resolveResult(String status) {
        return "PROCESSED".equalsIgnoreCase(status) ? ProcessedResult.SUCCESS : ProcessedResult.FAILED;
    }

    private ReasonCode resolveReasonCode(String reasonCode) {
        if (reasonCode == null || reasonCode.isBlank()) {
            return null;
        }
        try {
            return ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException ex) {
            return ReasonCode.INTEGRATION_FAILURE;
        }
    }
}
