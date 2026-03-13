package com.humanizar.acolhimento.domain.port.peding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.enums.Status;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;

public interface PendingTargetStatusPort {

    PendingTargetStatus save(PendingTargetStatus targetStatus);

    List<PendingTargetStatus> saveAll(List<PendingTargetStatus> targetStatuses);

    Optional<PendingTargetStatus> findByEventIdAndTargetService(UUID eventId, String targetService);

    List<PendingTargetStatus> findByEventId(UUID eventId);

    List<PendingTargetStatus> findByTargetServiceAndStatus(String targetService, Status status);

    void deleteByEventId(UUID eventId);
}
