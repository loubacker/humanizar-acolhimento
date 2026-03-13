package com.humanizar.acolhimento.infrastructure.adapter.acolhimento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;
import com.humanizar.acolhimento.infrastructure.persistence.entity.acolhimento.AcolhimentoEntity;
import com.humanizar.acolhimento.infrastructure.persistence.repository.acolhimento.AcolhimentoRepository;

@Component
public class AcolhimentoAdapter implements AcolhimentoPort {

    private final AcolhimentoRepository acolhimentoRepository;

    public AcolhimentoAdapter(AcolhimentoRepository acolhimentoRepository) {
        this.acolhimentoRepository = acolhimentoRepository;
    }

    @Override
    @Transactional
    public Acolhimento save(Acolhimento acolhimento) {
        AcolhimentoEntity saved = acolhimentoRepository.save(toEntity(acolhimento));
        return toDomain(saved);
    }

    @Override
    public Optional<Acolhimento> findByPatientId(UUID patientId) {
        return acolhimentoRepository.findByPatientId(patientId).map(this::toDomain);
    }

    @Override
    public boolean existsByPatientId(UUID patientId) {
        return acolhimentoRepository.existsByPatientId(patientId);
    }

    @Override
    @Transactional
    public void deleteById(UUID acolhimentoId) {
        if (acolhimentoId == null) {
            return;
        }
        acolhimentoRepository.deleteById(acolhimentoId);
    }

    @Override
    @Transactional
    public void deleteByPatientId(UUID patientId) {
        acolhimentoRepository.deleteByPatientId(patientId);
    }

    private Acolhimento toDomain(AcolhimentoEntity entity) {
        return new Acolhimento(
                entity.getId(),
                entity.getPatientId(),
                entity.getDataAnamnese(),
                entity.getHoraAnamnese(),
                entity.getObservacoes(),
                List.of(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private AcolhimentoEntity toEntity(Acolhimento domain) {
        AcolhimentoEntity entity = new AcolhimentoEntity();
        entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
        entity.setPatientId(domain.getPatientId());
        entity.setDataAnamnese(domain.getDataAnamnese());
        entity.setHoraAnamnese(domain.getHoraAnamnese());
        entity.setObservacoes(domain.getObservacoes());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
