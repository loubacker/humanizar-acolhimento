package com.humanizar.acolhimento.domain.port.acolhimento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;

public interface AcolhimentoPort {

    Acolhimento save(Acolhimento acolhimento);

    Optional<Acolhimento> findByNucleoPatientId(UUID nucleoPatientId);

    boolean existsByNucleoPatientId(UUID nucleoPatientId);

    List<Acolhimento> findByNucleoPatientIdIn(List<UUID> nucleoPatientIds);

    void deleteById(UUID acolhimentoId);

    void deleteByNucleoPatientId(UUID nucleoPatientId);

    void deleteByPatientId(UUID patientId);
}
