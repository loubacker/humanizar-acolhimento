package com.humanizar.acolhimento.infrastructure.adapter.nucleo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;
import com.humanizar.acolhimento.infrastructure.persistence.entity.nucleo.NucleoPatientEntity;
import com.humanizar.acolhimento.infrastructure.persistence.repository.nucleo.NucleoPatientRepository;

@Component
public class NucleoPatientAdapter implements NucleoPatientPort {

    private final NucleoPatientRepository nucleoPatientRepository;

    public NucleoPatientAdapter(NucleoPatientRepository nucleoPatientRepository) {
        this.nucleoPatientRepository = nucleoPatientRepository;
    }

    @Override
    @Transactional
    public List<NucleoPatient> saveAll(List<NucleoPatient> nucleos) {
        if (nucleos == null || nucleos.isEmpty()) {
            return List.of();
        }
        return nucleoPatientRepository.saveAll(nucleos.stream().map(this::toEntity).toList())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<NucleoPatient> findByPatientId(UUID patientId) {
        return nucleoPatientRepository.findByPatientId(patientId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<NucleoPatient> findByPatientIdAndNucleoId(UUID patientId, UUID nucleoId) {
        return nucleoPatientRepository.findByPatientIdAndNucleoId(patientId, nucleoId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteByPatientId(UUID patientId) {
        nucleoPatientRepository.deleteByPatientId(patientId);
    }

    @Override
    @Transactional
    public void deleteByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        nucleoPatientRepository.deleteByIdIn(ids);
    }

    @Override
    @Transactional
    public void deleteByPatientIdAndNucleoId(UUID patientId, UUID nucleoId) {
        nucleoPatientRepository.deleteByPatientIdAndNucleoId(patientId, nucleoId);
    }

    private NucleoPatient toDomain(NucleoPatientEntity entity) {
        return new NucleoPatient(
                entity.getId(),
                entity.getPatientId(),
                entity.getNucleoId(),
                List.of());
    }

    private NucleoPatientEntity toEntity(NucleoPatient domain) {
        UUID id = Objects.requireNonNull(domain.getId(),
                "id (nucleoPatientId) e obrigatorio para persistir NucleoPatient");

        NucleoPatientEntity entity = new NucleoPatientEntity();
        entity.setId(id);
        entity.setPatientId(domain.getPatientId());
        entity.setNucleoId(domain.getNucleoId());
        return entity;
    }
}
