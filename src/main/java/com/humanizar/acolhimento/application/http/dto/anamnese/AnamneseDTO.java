package com.humanizar.acolhimento.application.http.dto.anamnese;

import java.time.LocalDate;
import java.time.LocalTime;

public record AnamneseDTO(
        LocalDate dataAnamnese,
        LocalTime horaAnamnese,
        String observacoes) {
}
