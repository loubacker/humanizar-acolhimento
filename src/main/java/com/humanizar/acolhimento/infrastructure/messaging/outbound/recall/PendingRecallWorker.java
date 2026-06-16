package com.humanizar.acolhimento.infrastructure.messaging.outbound.recall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PendingRecallWorker {

    private static final Logger log = LoggerFactory.getLogger(PendingRecallWorker.class);
    private static final int BATCH_SIZE = 50;

    private final PendingRecall pendingRecall;

    public PendingRecallWorker(PendingRecall pendingRecall) {
        this.pendingRecall = pendingRecall;
    }

    @Scheduled(fixedDelay = 30_000)
    public void recall() {
        try {
            pendingRecall.recall(BATCH_SIZE);
        } catch (Exception ex) {
            log.error("Falha no ciclo de recall de pendentes", ex);
        }
    }
}
