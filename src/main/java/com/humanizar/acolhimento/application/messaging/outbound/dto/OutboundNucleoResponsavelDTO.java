package com.humanizar.acolhimento.application.messaging.outbound.dto;

import java.util.UUID;

public record OutboundNucleoResponsavelDTO(
                UUID nucleoPatientId,
                UUID responsavelId,
                String role) {
}
