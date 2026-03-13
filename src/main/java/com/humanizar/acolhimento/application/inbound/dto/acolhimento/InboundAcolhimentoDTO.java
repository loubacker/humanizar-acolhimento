package com.humanizar.acolhimento.application.inbound.dto.acolhimento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;

public record InboundAcolhimentoDTO(
        UUID patientId,
        LocalDate dataAnamnese,
        LocalTime horaAnamnese,
        String observacoes,
        List<NucleoPatientDTO> nucleoPatient) {
}
