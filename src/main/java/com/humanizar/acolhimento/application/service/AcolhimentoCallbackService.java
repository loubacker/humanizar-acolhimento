package com.humanizar.acolhimento.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.outbound.CallbackDTO;
import com.humanizar.acolhimento.application.usecase.callback.FinalizePendingAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.callback.CheckDuplicateEventUseCase;
import com.humanizar.acolhimento.application.usecase.callback.SaveProcessedEventUseCase;
import com.humanizar.acolhimento.application.usecase.callback.UpdateCallbackUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.Status;

@Service
public class AcolhimentoCallbackService {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoCallbackService.class);

    private final CheckDuplicateEventUseCase checkDuplicateEventUseCase;
    private final UpdateCallbackUseCase updatePendingTargetStatusFromCallbackUseCase;
    private final FinalizePendingAcolhimentoUseCase finalizePendingAcolhimentoUseCase;
    private final SaveProcessedEventUseCase saveProcessedEventUseCase;

    public AcolhimentoCallbackService(
            CheckDuplicateEventUseCase checkDuplicateEventUseCase,
            UpdateCallbackUseCase updatePendingTargetStatusFromCallbackUseCase,
            FinalizePendingAcolhimentoUseCase finalizePendingAcolhimentoUseCase,
            SaveProcessedEventUseCase saveProcessedEventUseCase) {
        this.checkDuplicateEventUseCase = checkDuplicateEventUseCase;
        this.updatePendingTargetStatusFromCallbackUseCase = updatePendingTargetStatusFromCallbackUseCase;
        this.finalizePendingAcolhimentoUseCase = finalizePendingAcolhimentoUseCase;
        this.saveProcessedEventUseCase = saveProcessedEventUseCase;
    }

    public void processCallback(String consumerName, String targetService, CallbackDTO callback) {
        validateCallback(callback);

        String correlationId = callback.correlationId() != null ? callback.correlationId().toString() : null;
        try {
            checkDuplicateEventUseCase.execute(consumerName, callback.eventId(), correlationId);
        } catch (AcolhimentoException ex) {
            if (ex.getReasonCode() == ReasonCode.DUPLICATE_EVENT) {
                log.info("callback duplicado ignorado. consumer={}, eventId={}", consumerName, callback.eventId());
                return;
            }
            throw ex;
        }

        updatePendingTargetStatusFromCallbackUseCase.execute(
                callback.eventId(),
                targetService,
                resolveTargetStatus(callback.status()));
        finalizePendingAcolhimentoUseCase.execute(callback.eventId());

        saveProcessedEventUseCase.execute(consumerName, callback);
    }

    private Status resolveTargetStatus(String callbackStatus) {
        return "PROCESSED".equalsIgnoreCase(callbackStatus) ? Status.SUCCESS : Status.ERROR;
    }

    private void validateCallback(CallbackDTO callback) {
        if (callback == null) {
            throw new AcolhimentoException(ReasonCode.VALIDATION_ERROR, null, "callback e obrigatorio");
        }
        if (callback.eventId() == null) {
            throw new AcolhimentoException(
                    ReasonCode.VALIDATION_ERROR,
                    callback.correlationId() != null ? callback.correlationId().toString() : null,
                    "callback.eventId e obrigatorio");
        }
        if (callback.status() == null || callback.status().isBlank()) {
            throw new AcolhimentoException(
                    ReasonCode.VALIDATION_ERROR,
                    callback.correlationId() != null ? callback.correlationId().toString() : null,
                    "callback.status e obrigatorio");
        }
    }
}
