package com.humanizar.acolhimento.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.infrastructure.persistence.entity.ProcessedEventEntity;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {

    boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId);

    Optional<ProcessedEventEntity> findByConsumerNameAndEventId(String consumerName, UUID eventId);

    List<ProcessedEventEntity> findByEventId(UUID eventId);

    List<ProcessedEventEntity> findByCorrelationId(UUID correlationId);

    List<ProcessedEventEntity> findBySourceRoutingKey(String sourceRoutingKey);

    void deleteByProcessedAtBefore(LocalDateTime cutoff);
}
