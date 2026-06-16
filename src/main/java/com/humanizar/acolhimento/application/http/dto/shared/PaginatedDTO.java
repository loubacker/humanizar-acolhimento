package com.humanizar.acolhimento.application.http.dto.shared;

import java.util.List;

public record PaginatedDTO<T>(
        long totalElements,
        int page,
        int size,
        int totalPages,
        List<T> data) {
}
