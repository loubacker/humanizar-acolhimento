package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.model.OutboxEvent;

@Component
public class OutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayWorker.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_PARALLELISM = 8;

    private final OutboxEventProcessor outboxEventProcessor;
    private final Executor outboxExecutor;

    public OutboxRelayWorker(
            OutboxEventProcessor outboxEventProcessor,
            @Qualifier("outboxExecutor") Executor outboxExecutor) {
        this.outboxEventProcessor = outboxEventProcessor;
        this.outboxExecutor = outboxExecutor;
    }

    @Scheduled(fixedDelay = 5_000)
    public void relay() {
        List<OutboxEvent> claimed = outboxEventProcessor.claimBatch(BATCH_SIZE);
        if (claimed.isEmpty()) {
            return;
        }

        Semaphore semaphore = new Semaphore(MAX_PARALLELISM);
        List<CompletableFuture<Void>> tasks = new ArrayList<>(claimed.size());

        for (OutboxEvent event : claimed) {
            try {
                semaphore.acquire();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Relay interrompido ao adquirir semaforo. eventId={}", event.getEventId(), ex);
                break;
            }

            CompletableFuture<Void> task = CompletableFuture
                    .runAsync(() -> outboxEventProcessor.processEvent(event), outboxExecutor)
                    .whenComplete((unused, throwable) -> {
                        semaphore.release();
                        if (throwable != null) {
                            log.error("Falha no processamento assíncrono do outbox. eventId={}",
                                    event.getEventId(), throwable);
                        }
                    });

            tasks.add(task);
        }

        if (!tasks.isEmpty()) {
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
        }
    }
}
