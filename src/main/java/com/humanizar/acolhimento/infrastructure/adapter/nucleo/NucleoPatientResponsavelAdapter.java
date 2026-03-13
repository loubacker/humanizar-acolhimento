package com.humanizar.acolhimento.infrastructure.adapter.nucleo;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientResponsavelPort;
import com.humanizar.acolhimento.infrastructure.persistence.entity.nucleo.NucleoPatientResponsavelEntity;
import com.humanizar.acolhimento.infrastructure.persistence.repository.nucleo.NucleoPatientResponsavelRepository;

@Component
public class NucleoPatientResponsavelAdapter implements NucleoPatientResponsavelPort {

    private final NucleoPatientResponsavelRepository repository;

    public NucleoPatientResponsavelAdapter(NucleoPatientResponsavelRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public List<NucleoPatientResponsavel> saveAll(List<NucleoPatientResponsavel> responsaveis) {
        if (responsaveis == null || responsaveis.isEmpty()) {
            return List.of();
        }
        return repository.saveAll(responsaveis.stream().map(this::toEntity).toList())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<NucleoPatientResponsavel> findByNucleoPatientId(UUID nucleoPatientId) {
        return repository.findByNucleoPatientId(nucleoPatientId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByNucleoPatientId(UUID nucleoPatientId) {
        repository.deleteByNucleoPatientId(nucleoPatientId);
    }

    @Override
    @Transactional
    public void deleteByNucleoPatientIds(List<UUID> nucleoPatientIds) {
        if (nucleoPatientIds == null || nucleoPatientIds.isEmpty()) {
            return;
        }
        repository.deleteByNucleoPatientIds(nucleoPatientIds);
    }

    @Override
    @Transactional
    public void deleteByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteByIdIn(ids);
    }

    @Override
    @Transactional
    public void deleteByPatientId(UUID patientId) {
        repository.deleteByPatientId(patientId);
    }

    private NucleoPatientResponsavel toDomain(NucleoPatientResponsavelEntity entity) {
        return new NucleoPatientResponsavel(
                entity.getId(),
                entity.getNucleoPatientId(),
                entity.getResponsavelId(),
                entity.getRole());
    }

    private NucleoPatientResponsavelEntity toEntity(NucleoPatientResponsavel domain) {
        UUID id = Objects.requireNonNull(domain.getId(),
                "id e obrigatorio para persistir NucleoPatientResponsavel");
        UUID nucleoPatientId = Objects.requireNonNull(domain.getNucleoPatientId(),
                "nucleoPatientId e obrigatorio para persistir NucleoPatientResponsavel");

        NucleoPatientResponsavelEntity entity = new NucleoPatientResponsavelEntity();
        entity.setId(id);
        entity.setNucleoPatientId(nucleoPatientId);
        entity.setResponsavelId(domain.getResponsavelId());
        entity.setRole(domain.getRole());
        return entity;
    }
}
