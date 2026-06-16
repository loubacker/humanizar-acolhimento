package com.humanizar.acolhimento.application.http.dto.nucleo;

import java.util.UUID;

public record NucleoResponsavelDTO(
        UUID responsavelId,
        String role) {
}
