package com.humanizar.acolhimento.infrastructure.adapter.peding;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;
import com.humanizar.acolhimento.domain.port.peding.PendingTargetStatusPort;
import com.humanizar.acolhimento.infrastructure.persistence.entity.peding.PendingTargetStatusEntity;
import com.humanizar.acolhimento.infrastructure.persistence.repository.peding.PendingTargetStatusRepository;

@Component
public class PendingTargetStatusAdapter implements PendingTargetStatusPort {

    private final PendingTargetStatusRepository repository;

    public PendingTargetStatusAdapter(PendingTargetStatusRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public PendingTargetStatus save(PendingTargetStatus targetStatus) {
        PendingTargetStatusEntity saved = repository.save(toEntity(targetStatus));
        return toDomain(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<PendingTargetStatus> saveAll(List<PendingTargetStatus> targetStatuses) {
        if (targetStatuses == null || targetStatuses.isEmpty()) {
            return List.of();
        }
        return repository.saveAll(targetStatuses.stream().map(this::toEntity).toList())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<PendingTargetStatus> findByEventIdAndTargetService(UUID eventId, String targetService) {
        return repository.findByEventIdAndTargetService(eventId, targetService)
                .map(this::toDomain);
    }

    @Override
    public List<PendingTargetStatus> findByEventId(UUID eventId) {
        return repository.findByEventId(eventId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PendingTargetStatus> findByTargetServiceAndStatus(String targetService, Status status) {
        return repository.findByTargetServiceAndStatus(targetService, status).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<PendingTargetStatus> findStaleTargets(Status status, LocalDateTime now, int limit) {
        return repository
                .findByStatusAndNextRecallAtLessThanEqualOrderByNextRecallAtAsc(
                        status, now, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteByEventId(UUID eventId) {
        repository.deleteByEventId(eventId);
    }

    private PendingTargetStatus toDomain(PendingTargetStatusEntity entity) {
        return new PendingTargetStatus(
                entity.getId(),
                entity.getEventId(),
                entity.getTargetService(),
                entity.getStatus(),
                entity.getRecallAttempts(),
                entity.getLastRecallAt(),
                entity.getNextRecallAt());
    }

    private PendingTargetStatusEntity toEntity(PendingTargetStatus domain) {
        PendingTargetStatusEntity entity = new PendingTargetStatusEntity();
        entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
        entity.setEventId(domain.getEventId());
        entity.setTargetService(domain.getTargetService());
        entity.setStatus(domain.getStatus());
        entity.setRecallAttempts(domain.getRecallAttempts());
        entity.setLastRecallAt(domain.getLastRecallAt());
        entity.setNextRecallAt(domain.getNextRecallAt());
        return entity;
    }
}
