package com.humanizar.acolhimento.application.inbound.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.ResponsavelRole;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;

@Component
public class NucleoPatientInboundMapper {

    private static final String NUCLEO_PATIENT_FIELD = "acolhimento.nucleoPatient";

    public Map<UUID, UUID> reconcileIdsForCreate(List<NucleoPatientDTO> dtos, String correlationId) {
        List<NucleoPatientDTO> validated = validateNucleos(dtos, correlationId);
        Map<UUID, UUID> idsByNucleoId = new LinkedHashMap<>();
        for (NucleoPatientDTO dto : validated) {
            idsByNucleoId.put(dto.nucleoId(), UUID.randomUUID());
        }
        return idsByNucleoId;
    }

    public Map<UUID, UUID> reconcileIdsForUpdate(
            List<NucleoPatientDTO> dtos,
            Map<UUID, UUID> existingIdsByNucleoId,
            String correlationId) {
        List<NucleoPatientDTO> validated = validateNucleos(dtos, correlationId);
        Map<UUID, UUID> existing = existingIdsByNucleoId != null ? existingIdsByNucleoId : Map.of();
        Map<UUID, UUID> idsByNucleoId = new LinkedHashMap<>();
        for (NucleoPatientDTO dto : validated) {
            UUID existingId = existing.get(dto.nucleoId());
            idsByNucleoId.put(dto.nucleoId(), existingId != null ? existingId : UUID.randomUUID());
        }
        return idsByNucleoId;
    }

    public List<NucleoPatient> toDomainList(
            List<NucleoPatientDTO> dtos,
            UUID patientId,
            Map<UUID, UUID> idsByNucleoId,
            String correlationId) {
        requireField(patientId, "acolhimento.patientId", correlationId);
        List<NucleoPatientDTO> validated = validateNucleos(dtos, correlationId);
        Map<UUID, UUID> resolvedIds = requireField(idsByNucleoId, "nucleoPatientIdsByNucleoId", correlationId);

        List<NucleoPatient> result = new ArrayList<>(validated.size());
        for (NucleoPatientDTO dto : validated) {
            UUID nucleoPatientId = requireField(
                    resolvedIds.get(dto.nucleoId()),
                    "nucleoPatientIdsByNucleoId[" + dto.nucleoId() + "]",
                    correlationId);

            result.add(NucleoPatient.builder()
                    .id(nucleoPatientId)
                    .patientId(patientId)
                    .nucleoId(dto.nucleoId())
                    .nucleoPatientResponsavel(List.of())
                    .build());
        }

        return result;
    }

    public List<NucleoPatientResponsavel> toResponsavelList(
            List<NucleoPatientDTO> dtos,
            Map<UUID, UUID> idsByNucleoId,
            String correlationId) {
        List<NucleoPatientDTO> validated = validateNucleos(dtos, correlationId);
        Map<UUID, UUID> resolvedIds = requireField(idsByNucleoId, "nucleoPatientIdsByNucleoId", correlationId);

        List<NucleoPatientResponsavel> result = new ArrayList<>();
        for (int i = 0; i < validated.size(); i++) {
            NucleoPatientDTO dto = validated.get(i);
            UUID nucleoPatientId = requireField(
                    resolvedIds.get(dto.nucleoId()),
                    "nucleoPatientIdsByNucleoId[" + dto.nucleoId() + "]",
                    correlationId);

            List<NucleoResponsavelDTO> responsaveis = validateResponsaveis(
                    dto.nucleoPatientResponsavel(),
                    "acolhimento.nucleoPatient[" + i + "].nucleoPatientResponsavel",
                    correlationId);

            for (int j = 0; j < responsaveis.size(); j++) {
                NucleoResponsavelDTO responsavel = responsaveis.get(j);
                ResponsavelRole role = parseRole(
                        requireText(
                                responsavel.role(),
                                "acolhimento.nucleoPatient[" + i + "].nucleoPatientResponsavel[" + j + "].role",
                                correlationId),
                        "acolhimento.nucleoPatient[" + i + "].nucleoPatientResponsavel[" + j + "].role",
                        correlationId);

                result.add(NucleoPatientResponsavel.builder()
                        .id(UUID.randomUUID())
                        .nucleoPatientId(nucleoPatientId)
                        .responsavelId(requireField(
                                responsavel.responsavelId(),
                                "acolhimento.nucleoPatient[" + i + "].nucleoPatientResponsavel[" + j
                                        + "].responsavelId",
                                correlationId))
                        .role(role)
                        .build());
            }
        }

        return result;
    }

    private List<NucleoPatientDTO> validateNucleos(List<NucleoPatientDTO> dtos, String correlationId) {
        List<NucleoPatientDTO> safeDtos = requireNonEmpty(dtos, NUCLEO_PATIENT_FIELD, correlationId);
        Set<UUID> nucleoIds = new HashSet<>();

        for (int i = 0; i < safeDtos.size(); i++) {
            String itemField = NUCLEO_PATIENT_FIELD + "[" + i + "]";
            NucleoPatientDTO dto = requireField(safeDtos.get(i), itemField, correlationId);
            UUID nucleoId = requireField(dto.nucleoId(), itemField + ".nucleoId", correlationId);

            if (!nucleoIds.add(nucleoId)) {
                throw new AcolhimentoException(
                        ReasonCode.INBOUND_DUPLICATE_ITEM,
                        correlationId,
                        "Item duplicado no inbound: " + itemField + ".nucleoId");
            }

            validateResponsaveis(dto.nucleoPatientResponsavel(), itemField + ".nucleoPatientResponsavel",
                    correlationId);
        }

        return safeDtos;
    }

    private List<NucleoResponsavelDTO> validateResponsaveis(
            List<NucleoResponsavelDTO> dtos,
            String field,
            String correlationId) {
        List<NucleoResponsavelDTO> safeDtos = requireNonEmpty(dtos, field, correlationId);
        Set<UUID> responsavelIds = new HashSet<>();

        for (int i = 0; i < safeDtos.size(); i++) {
            String itemField = field + "[" + i + "]";
            NucleoResponsavelDTO dto = requireField(safeDtos.get(i), itemField, correlationId);
            UUID responsavelId = requireField(dto.responsavelId(), itemField + ".responsavelId", correlationId);
            String role = requireText(dto.role(), itemField + ".role", correlationId);

            if (!responsavelIds.add(responsavelId)) {
                throw new AcolhimentoException(
                        ReasonCode.INBOUND_DUPLICATE_ITEM,
                        correlationId,
                        "Item duplicado no inbound: " + itemField + ".responsavelId");
            }

            parseRole(role, itemField + ".role", correlationId);
        }

        return safeDtos;
    }

    private ResponsavelRole parseRole(String role, String field, String correlationId) {
        try {
            return ResponsavelRole.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_INVALID_ENUM,
                    correlationId,
                    "Valor invalido para " + field + ": " + role);
        }
    }

    private String requireText(String value, String field, String correlationId) {
        if (value == null || value.isBlank()) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_REQUIRED_FIELD,
                    correlationId,
                    "Campo obrigatorio ausente: " + field);
        }
        return value;
    }

    private <T> List<T> requireNonEmpty(List<T> value, String field, String correlationId) {
        if (value == null || value.isEmpty()) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_EMPTY_COLLECTION,
                    correlationId,
                    "Colecao obrigatoria vazia: " + field);
        }
        return value;
    }

    private <T> T requireField(T value, String field, String correlationId) {
        if (value == null) {
            throw new AcolhimentoException(
                    ReasonCode.INBOUND_REQUIRED_FIELD,
                    correlationId,
                    "Campo obrigatorio ausente: " + field);
        }
        return value;
    }
}
