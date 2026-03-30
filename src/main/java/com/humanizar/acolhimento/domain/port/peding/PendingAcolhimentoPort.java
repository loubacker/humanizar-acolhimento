package com.humanizar.acolhimento.domain.port.peding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.humanizar.acolhimento.domain.model.enums.OperationType;
import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;

public interface PendingAcolhimentoPort {

    PendingAcolhimento save(PendingAcolhimento pending);

    Optional<PendingAcolhimento> findByEventId(UUID eventId);

    List<PendingAcolhimento> findByCorrelationId(UUID correlationId);

    List<PendingAcolhimento> findByPatientId(UUID patientId);

    Page<PendingAcolhimento> findByPatientId(UUID patientId, Pageable pageable);

    List<PendingAcolhimento> findByStatusInOrderByCreatedAtAsc(List<Status> status);

    boolean checkDeleteStatusByPatientId(UUID patientId, OperationType operationType, Status status);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
