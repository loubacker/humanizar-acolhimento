package com.humanizar.acolhimento.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.ProcessedEvent;

public interface ProcessedEventPort {

    ProcessedEvent save(ProcessedEvent processedEvent);

    boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId);

    Optional<ProcessedEvent> findByConsumerNameAndEventId(String consumerName, UUID eventId);

    List<ProcessedEvent> findByEventId(UUID eventId);

    List<ProcessedEvent> findByCorrelationId(UUID correlationId);

    List<ProcessedEvent> findBySourceRoutingKey(String sourceRoutingKey);

    void deleteByProcessedAtBefore(LocalDateTime cutoff);
}
