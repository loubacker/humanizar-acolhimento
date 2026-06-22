package com.humanizar.acolhimento.infrastructure.persistence.repository.peding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.infrastructure.persistence.entity.peding.PendingTargetStatusEntity;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface PendingTargetStatusRepository extends JpaRepository<PendingTargetStatusEntity, UUID> {

    Optional<PendingTargetStatusEntity> findByEventIdAndTargetService(UUID eventId, String targetService);

    List<PendingTargetStatusEntity> findByEventId(UUID eventId);

    List<PendingTargetStatusEntity> findByTargetServiceAndStatus(String targetService, Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<PendingTargetStatusEntity> findByStatusAndNextRecallAtLessThanEqualOrderByNextRecallAtAsc(
            Status status, LocalDateTime now, Pageable pageable);

    void deleteByEventId(UUID eventId);

    void deleteByStatusInAndCreatedAtBefore(List<Status> status, LocalDateTime cutoff);
}
