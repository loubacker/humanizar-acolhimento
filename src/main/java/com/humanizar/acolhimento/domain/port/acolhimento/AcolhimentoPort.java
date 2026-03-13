package com.humanizar.acolhimento.domain.port.acolhimento;

import java.util.Optional;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;

public interface AcolhimentoPort {

    Acolhimento save(Acolhimento acolhimento);

    Optional<Acolhimento> findByPatientId(UUID patientId);

    boolean existsByPatientId(UUID patientId);

    void deleteById(UUID acolhimentoId);

    void deleteByPatientId(UUID patientId);
}
