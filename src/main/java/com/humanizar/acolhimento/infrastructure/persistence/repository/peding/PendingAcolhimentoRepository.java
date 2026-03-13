package com.humanizar.acolhimento.infrastructure.persistence.repository.peding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.infrastructure.persistence.entity.peding.PendingAcolhimentoEntity;

@Repository
public interface PendingAcolhimentoRepository extends JpaRepository<PendingAcolhimentoEntity, UUID> {

    Optional<PendingAcolhimentoEntity> findByEventId(UUID eventId);

    List<PendingAcolhimentoEntity> findByCorrelationId(UUID correlationId);

    List<PendingAcolhimentoEntity> findByPatientId(UUID patientId);

    List<PendingAcolhimentoEntity> findByStatusInOrderByCreatedAtAsc(List<Status> status);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
