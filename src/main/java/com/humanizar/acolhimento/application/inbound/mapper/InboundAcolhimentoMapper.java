package com.humanizar.acolhimento.application.inbound.mapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoMappingResult;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

@Component
public class InboundAcolhimentoMapper {

    private final NucleoPatientInboundMapper nucleoPatientInboundMapper;

    public InboundAcolhimentoMapper(NucleoPatientInboundMapper nucleoPatientInboundMapper) {
        this.nucleoPatientInboundMapper = nucleoPatientInboundMapper;
    }

    public InboundAcolhimentoDTO toCreatePayload(InboundAcolhimentoDTO inboundAcolhimentoDTO) {
        return requireField(inboundAcolhimentoDTO, "payload", null);
    }

    public InboundAcolhimentoDTO toUpdatePayload(InboundAcolhimentoDTO inboundAcolhimentoDTO) {
        return requireField(inboundAcolhimentoDTO, "payload", null);
    }

    public AcolhimentoDeleteDTO toDeletePayload(AcolhimentoDeleteDTO acolhimentoDeleteDTO) {
        return requireField(acolhimentoDeleteDTO, "payload", null);
    }

    public InboundAcolhimentoMappingResult mapCreate(
            InboundAcolhimentoDTO payload,
            String correlationId) {
        return mapCreate(payload, UUID.randomUUID(), correlationId);
    }

    public InboundAcolhimentoMappingResult mapCreate(
            InboundAcolhimentoDTO payload,
            UUID acolhimentoId,
            String correlationId) {
        InboundAcolhimentoDTO safePayload = validateAcolhimentoPayload(payload, correlationId);
        UUID safeAcolhimentoId = requireField(acolhimentoId, "acolhimento.id", correlationId);

        Map<UUID, UUID> idsByNucleoId = nucleoPatientInboundMapper.reconcileIdsForCreate(
                safePayload.nucleoPatient(),
                correlationId);

        return buildResult(safePayload, safeAcolhimentoId, idsByNucleoId, correlationId);
    }

    public InboundAcolhimentoMappingResult mapUpdate(
            InboundAcolhimentoDTO payload,
            Map<UUID, UUID> existingIdsByNucleoId,
            String correlationId) {
        return mapUpdate(payload, null, existingIdsByNucleoId, correlationId);
    }

    public InboundAcolhimentoMappingResult mapUpdate(
            InboundAcolhimentoDTO payload,
            UUID acolhimentoId,
            Map<UUID, UUID> existingIdsByNucleoId,
            String correlationId) {
        InboundAcolhimentoDTO safePayload = validateAcolhimentoPayload(payload, correlationId);

        Map<UUID, UUID> idsByNucleoId = nucleoPatientInboundMapper.reconcileIdsForUpdate(
                safePayload.nucleoPatient(),
                existingIdsByNucleoId,
                correlationId);

        return buildResult(safePayload, acolhimentoId, idsByNucleoId, correlationId);
    }

    private InboundAcolhimentoMappingResult buildResult(
            InboundAcolhimentoDTO payload,
            UUID acolhimentoId,
            Map<UUID, UUID> idsByNucleoId,
            String correlationId) {
        LocalDateTime now = LocalDateTime.now();

        Acolhimento acolhimento = Acolhimento.builder()
                .id(acolhimentoId)
                .patientId(payload.patientId())
                .dataAnamnese(payload.dataAnamnese())
                .horaAnamnese(payload.horaAnamnese())
                .observacoes(payload.observacoes())
                .createdAt(now)
                .updatedAt(now)
                .build();

        var nucleoPatients = nucleoPatientInboundMapper.toDomainList(
                payload.nucleoPatient(),
                payload.patientId(),
                idsByNucleoId,
                correlationId);

        var responsaveis = nucleoPatientInboundMapper.toResponsavelList(
                payload.nucleoPatient(),
                idsByNucleoId,
                correlationId);

        return new InboundAcolhimentoMappingResult(
                acolhimento,
                nucleoPatients,
                responsaveis,
                idsByNucleoId);
    }

    private InboundAcolhimentoDTO validateAcolhimentoPayload(
            InboundAcolhimentoDTO payload,
            String correlationId) {
        InboundAcolhimentoDTO safePayload = requireField(payload, "acolhimento", correlationId);
        requireField(safePayload.patientId(), "acolhimento.patientId", correlationId);
        requireField(safePayload.dataAnamnese(), "acolhimento.dataAnamnese", correlationId);
        requireField(safePayload.horaAnamnese(), "acolhimento.horaAnamnese", correlationId);
        requireField(safePayload.nucleoPatient(), "acolhimento.nucleoPatient", correlationId);
        return safePayload;
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
