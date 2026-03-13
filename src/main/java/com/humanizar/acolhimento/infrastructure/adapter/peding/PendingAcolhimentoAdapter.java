package com.humanizar.acolhimento.infrastructure.adapter.peding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingAcolhimento;
import com.humanizar.acolhimento.domain.port.peding.PendingAcolhimentoPort;
import com.humanizar.acolhimento.infrastructure.persistence.entity.peding.PendingAcolhimentoEntity;
import com.humanizar.acolhimento.infrastructure.persistence.repository.peding.PendingAcolhimentoRepository;

@Component
public class PendingAcolhimentoAdapter implements PendingAcolhimentoPort {

    private final PendingAcolhimentoRepository repository;

    public PendingAcolhimentoAdapter(PendingAcolhimentoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public PendingAcolhimento save(PendingAcolhimento pending) {
        PendingAcolhimentoEntity saved = repository.save(toEntity(pending));
        return toDomain(saved);
    }

    @Override
    public Optional<PendingAcolhimento> findByEventId(UUID eventId) {
        return repository.findByEventId(eventId).map(this::toDomain);
    }

    @Override
    public List<PendingAcolhimento> findByCorrelationId(UUID correlationId) {
        return repository.findByCorrelationId(correlationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PendingAcolhimento> findByPatientId(UUID patientId) {
        return repository.findByPatientId(patientId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PendingAcolhimento> findByStatusInOrderByCreatedAtAsc(List<Status> status) {
        return repository.findByStatusInOrderByCreatedAtAsc(status).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteByCreatedAtBefore(LocalDateTime cutoff) {
        repository.deleteByCreatedAtBefore(cutoff);
    }

    private PendingAcolhimento toDomain(PendingAcolhimentoEntity entity) {
        return new PendingAcolhimento(
                entity.getEventId(),
                entity.getCorrelationId(),
                entity.getPatientId(),
                entity.getOperationType(),
                entity.getStatus(),
                entity.getPayloadSnapshot(),
                entity.getCreatedAt());
    }

    private PendingAcolhimentoEntity toEntity(PendingAcolhimento domain) {
        PendingAcolhimentoEntity entity = new PendingAcolhimentoEntity();
        entity.setEventId(Objects.requireNonNull(domain.getEventId(),
                "PendingAcolhimento.eventId must be provided and is the PK"));
        entity.setCorrelationId(domain.getCorrelationId());
        entity.setPatientId(domain.getPatientId());
        entity.setOperationType(domain.getOperationType());
        entity.setStatus(domain.getStatus());
        entity.setPayloadSnapshot(domain.getPayloadSnapshot());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
