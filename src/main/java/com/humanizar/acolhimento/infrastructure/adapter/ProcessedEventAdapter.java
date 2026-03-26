package com.humanizar.acolhimento.infrastructure.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.ProcessedEvent;
import com.humanizar.acolhimento.domain.port.ProcessedEventPort;
import com.humanizar.acolhimento.infrastructure.persistence.entity.ProcessedEventEntity;
import com.humanizar.acolhimento.infrastructure.persistence.repository.ProcessedEventRepository;

@Component
public class ProcessedEventAdapter implements ProcessedEventPort {

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventAdapter(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        ProcessedEventEntity entity = toEntity(processedEvent);
        ProcessedEventEntity saved = processedEventRepository.save(entity);
        return toDomain(Objects.requireNonNull(saved, "Erro ao salvar processed event"));
    }

    @Override
    public boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId) {
        return processedEventRepository.existsByConsumerNameAndEventId(consumerName, eventId);
    }

    @Override
    public Optional<ProcessedEvent> findByConsumerNameAndEventId(String consumerName, UUID eventId) {
        return processedEventRepository.findByConsumerNameAndEventId(consumerName, eventId)
                .map(this::toDomain);
    }

    @Override
    public List<ProcessedEvent> findByEventId(UUID eventId) {
        return processedEventRepository.findByEventId(eventId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ProcessedEvent> findByCorrelationId(UUID correlationId) {
        return processedEventRepository.findByCorrelationId(correlationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ProcessedEvent> findBySourceRoutingKey(String sourceRoutingKey) {
        return processedEventRepository.findBySourceRoutingKey(sourceRoutingKey).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByProcessedAtBefore(LocalDateTime cutoff) {
        processedEventRepository.deleteByProcessedAtBefore(cutoff);
    }

    private ProcessedEvent toDomain(ProcessedEventEntity entity) {
        return new ProcessedEvent(
                entity.getId(),
                entity.getConsumerName(),
                entity.getEventId(),
                entity.getCorrelationId(),
                entity.getSourceExchange(),
                entity.getSourceRoutingKey(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getActorId(),
                entity.getUserAgent(),
                entity.getOriginIp(),
                entity.getProcessedAt(),
                entity.getResult(),
                entity.getReasonCode(),
                entity.getErrorMessage());
    }

    private ProcessedEventEntity toEntity(ProcessedEvent domain) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setId(domain.getId());
        entity.setConsumerName(domain.getConsumerName());
        entity.setEventId(domain.getEventId());
        entity.setCorrelationId(domain.getCorrelationId());
        entity.setSourceExchange(domain.getSourceExchange());
        entity.setSourceRoutingKey(domain.getSourceRoutingKey());
        entity.setAggregateType(domain.getAggregateType());
        entity.setAggregateId(domain.getAggregateId());
        entity.setActorId(domain.getActorId());
        entity.setUserAgent(domain.getUserAgent());
        entity.setOriginIp(domain.getOriginIp());
        entity.setProcessedAt(domain.getProcessedAt());
        entity.setResult(domain.getResult());
        entity.setReasonCode(domain.getReasonCode());
        entity.setErrorMessage(domain.getErrorMessage());
        return entity;
    }
}
