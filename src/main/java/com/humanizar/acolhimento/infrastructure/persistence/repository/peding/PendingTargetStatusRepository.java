package com.humanizar.acolhimento.infrastructure.persistence.repository.peding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.infrastructure.persistence.entity.peding.PendingTargetStatusEntity;

@Repository
public interface PendingTargetStatusRepository extends JpaRepository<PendingTargetStatusEntity, UUID> {

    Optional<PendingTargetStatusEntity> findByEventIdAndTargetService(UUID eventId, String targetService);

    List<PendingTargetStatusEntity> findByEventId(UUID eventId);

    List<PendingTargetStatusEntity> findByTargetServiceAndStatus(String targetService, Status status);

    void deleteByEventId(UUID eventId);
}
