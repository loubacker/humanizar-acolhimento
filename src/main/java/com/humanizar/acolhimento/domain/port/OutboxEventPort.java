package com.humanizar.acolhimento.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;

public interface OutboxEventPort {

    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findByEventId(UUID eventId);

    List<OutboxEvent> findByCorrelationId(UUID correlationId);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, UUID aggregateId);

    List<OutboxEvent> findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            List<OutboxStatus> status, LocalDateTime cutoff);

    List<OutboxEvent> findPendingForRelay(List<OutboxStatus> status, LocalDateTime cutoff, int limit);

    void deleteByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime cutoff);
}
