package com.humanizar.acolhimento.infrastructure.persistence.repository.acolhimento;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.humanizar.acolhimento.infrastructure.persistence.entity.acolhimento.AcolhimentoEntity;

@Repository
public interface AcolhimentoRepository extends JpaRepository<AcolhimentoEntity, UUID> {

    Optional<AcolhimentoEntity> findByPatientId(UUID patientId);

    boolean existsByPatientId(UUID patientId);

    void deleteByPatientId(UUID patientId);
}
