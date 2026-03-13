package com.humanizar.acolhimento.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.infrastructure.persistence.entity.OutboxEventEntity;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

        Optional<OutboxEventEntity> findByEventId(UUID eventId);

        List<OutboxEventEntity> findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List<OutboxStatus> status, LocalDateTime cutoff);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
        List<OutboxEventEntity> findByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List<OutboxStatus> status, LocalDateTime cutoff, Pageable pageable);

        List<OutboxEventEntity> findByCorrelationId(UUID correlationId);

        List<OutboxEventEntity> findByAggregateTypeAndAggregateId(String aggregateType, UUID aggregateId);

        void deleteByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime cutoff);
}
