package com.humanizar.acolhimento.infrastructure.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;
import com.humanizar.acolhimento.infrastructure.persistence.entity.OutboxEventEntity;
import com.humanizar.acolhimento.infrastructure.persistence.repository.OutboxEventRepository;

@Component
public class OutboxEventAdapter implements OutboxEventPort {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventAdapter(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public OutboxEvent save(OutboxEvent outboxEvent) {
        OutboxEventEntity entity = toEntity(outboxEvent);
        OutboxEventEntity saved = outboxEventRepository.save(entity);
        return toDomain(Objects.requireNonNull(saved, "Erro ao salvar outbox event"));
    }

    @Override
    public Optional<OutboxEvent> findByEventId(UUID eventId) {
        return outboxEventRepository.findByEventId(eventId)
                .map(this::toDomain);
    }

    @Override
    public List<OutboxEvent> findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            List<OutboxStatus> status, LocalDateTime cutoff) {
        return outboxEventRepository
                .findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(status, cutoff)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<OutboxEvent> findPendingForRelay(List<OutboxStatus> status, LocalDateTime cutoff, int limit) {
        return outboxEventRepository
                .findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        status, cutoff, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<OutboxEvent> findByCorrelationId(UUID correlationId) {
        return outboxEventRepository.findByCorrelationId(correlationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, UUID aggregateId) {
        return outboxEventRepository.findByAggregateTypeAndAggregateId(aggregateType, aggregateId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime cutoff) {
        outboxEventRepository.deleteByStatusAndCreatedAtBefore(status, cutoff);
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getId(),
                entity.getEventId(),
                entity.getCorrelationId(),
                entity.getProducerService(),
                entity.getExchangeName(),
                entity.getRoutingKey(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventVersion(),
                entity.getPayload(),
                entity.getActorId(),
                entity.getUserAgent(),
                entity.getOriginIp(),
                entity.getStatus(),
                entity.getAttemptCount(),
                entity.getMaxAttempts(),
                entity.getNextRetryAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getPublishedAt(),
                entity.getLockedBy());
    }

    private OutboxEventEntity toEntity(OutboxEvent domain) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(domain.getId());
        entity.setEventId(domain.getEventId());
        entity.setCorrelationId(domain.getCorrelationId());
        entity.setProducerService(domain.getProducerService());
        entity.setExchangeName(domain.getExchangeName());
        entity.setRoutingKey(domain.getRoutingKey());
        entity.setAggregateType(domain.getAggregateType());
        entity.setAggregateId(domain.getAggregateId());
        entity.setEventVersion(domain.getEventVersion());
        entity.setPayload(domain.getPayload());
        entity.setActorId(domain.getActorId());
        entity.setUserAgent(domain.getUserAgent());
        entity.setOriginIp(domain.getOriginIp());
        entity.setStatus(domain.getStatus());
        entity.setAttemptCount(domain.getAttemptCount());
        entity.setMaxAttempts(domain.getMaxAttempts());
        entity.setNextRetryAt(domain.getNextRetryAt());
        entity.setLastError(domain.getLastError());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setPublishedAt(domain.getPublishedAt());
        entity.setLockedBy(domain.getLockedBy());
        return entity;
    }
}
