package com.humanizar.acolhimento.application.outbound.dto.central;

import java.util.List;

public record PendingCentralPageDTO(
        List<PendingCentralListDTO> data,
        int page,
        int size,
        int totalPages,
        long totalElements) {
}
