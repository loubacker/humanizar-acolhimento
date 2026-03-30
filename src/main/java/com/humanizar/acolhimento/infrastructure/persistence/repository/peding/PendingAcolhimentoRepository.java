package com.humanizar.acolhimento.infrastructure.persistence.repository.peding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.infrastructure.persistence.entity.peding.PendingAcolhimentoEntity;

@Repository
public interface PendingAcolhimentoRepository extends JpaRepository<PendingAcolhimentoEntity, UUID> {

    Optional<PendingAcolhimentoEntity> findByEventId(UUID eventId);

    List<PendingAcolhimentoEntity> findByCorrelationId(UUID correlationId);

    List<PendingAcolhimentoEntity> findByPatientId(UUID patientId);

    Page<PendingAcolhimentoEntity> findByPatientId(UUID patientId, Pageable pageable);

    List<PendingAcolhimentoEntity> findByStatusInOrderByCreatedAtAsc(List<Status> status);

    boolean existsByPatientIdAndOperationTypeAndStatus(UUID patientId, OperationType operationType, Status status);

    default boolean checkDeleteStatusByPatientId(UUID patientId, OperationType operationType, Status status) {
        return existsByPatientIdAndOperationTypeAndStatus(patientId, operationType, status);
    }

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
