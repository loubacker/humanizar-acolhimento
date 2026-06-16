package com.humanizar.acolhimento.application.http.dto.envelop;

import java.time.LocalDateTime;
import java.util.UUID;

public record HttpEnvelopeDTO<T>(
        UUID correlationId,
        String producerService,
        LocalDateTime occurredAt,
        UUID actorId,
        String userAgent,
        String originIp,
        T payload) {
}