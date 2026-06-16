package com.humanizar.acolhimento.application.http.dto.acolhimento;

import java.util.List;
import java.util.UUID;

import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoPatientDTO;

public record InboundAcolhimentoDTO(
        UUID patientId,
        List<NucleoPatientDTO> nucleoPatient) {
}
