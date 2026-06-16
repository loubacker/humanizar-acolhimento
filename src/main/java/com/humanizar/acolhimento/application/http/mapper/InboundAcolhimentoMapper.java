package com.humanizar.acolhimento.application.http.mapper;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoMapResultDTO;

@Component
public class InboundAcolhimentoMapper {

    private final NucleoPatientInboundMapper nucleoPatientInboundMapper;
    private final InboundFieldValidator fieldValidator;

    public InboundAcolhimentoMapper(
            NucleoPatientInboundMapper nucleoPatientInboundMapper,
            InboundFieldValidator fieldValidator) {
        this.nucleoPatientInboundMapper = nucleoPatientInboundMapper;
        this.fieldValidator = fieldValidator;
    }

    public InboundAcolhimentoDTO toCreatePayload(InboundAcolhimentoDTO inboundAcolhimentoDTO) {
        return fieldValidator.requireField(inboundAcolhimentoDTO, "payload", null);
    }

    public InboundAcolhimentoDTO toUpdatePayload(InboundAcolhimentoDTO inboundAcolhimentoDTO) {
        return fieldValidator.requireField(inboundAcolhimentoDTO, "payload", null);
    }

    public AcolhimentoDeleteDTO toDeletePayload(AcolhimentoDeleteDTO acolhimentoDeleteDTO) {
        return fieldValidator.requireField(acolhimentoDeleteDTO, "payload", null);
    }

    public InboundAcolhimentoMapResultDTO mapCreate(
            InboundAcolhimentoDTO payload,
            String correlationId) {
        InboundAcolhimentoDTO safePayload = validateAcolhimentoPayload(payload, correlationId);

        Map<UUID, UUID> idsByNucleoId = nucleoPatientInboundMapper.recallIdsForCreate(
                safePayload.nucleoPatient(),
                correlationId);

        return buildResult(safePayload, idsByNucleoId, correlationId);
    }

    public InboundAcolhimentoMapResultDTO mapUpdate(
            InboundAcolhimentoDTO payload,
            Map<UUID, UUID> existingIdsByNucleoId,
            String correlationId) {
        InboundAcolhimentoDTO safePayload = validateAcolhimentoPayload(payload, correlationId);

        Map<UUID, UUID> idsByNucleoId = nucleoPatientInboundMapper.recallIdsForUpdate(
                safePayload.nucleoPatient(),
                existingIdsByNucleoId,
                correlationId);

        return buildResult(safePayload, idsByNucleoId, correlationId);
    }

    private InboundAcolhimentoMapResultDTO buildResult(
            InboundAcolhimentoDTO payload,
            Map<UUID, UUID> idsByNucleoId,
            String correlationId) {
        var nucleoPatients = nucleoPatientInboundMapper.toDomainList(
                payload.nucleoPatient(),
                payload.patientId(),
                idsByNucleoId,
                correlationId);

        var responsaveis = nucleoPatientInboundMapper.toResponsavelList(
                payload.nucleoPatient(),
                idsByNucleoId,
                correlationId);

        return new InboundAcolhimentoMapResultDTO(
                payload.patientId(),
                nucleoPatients,
                responsaveis,
                idsByNucleoId);
    }

    private InboundAcolhimentoDTO validateAcolhimentoPayload(
            InboundAcolhimentoDTO payload,
            String correlationId) {
        InboundAcolhimentoDTO safePayload = fieldValidator.requireField(payload, "acolhimento", correlationId);
        fieldValidator.requireField(safePayload.patientId(), "acolhimento.patientId", correlationId);
        fieldValidator.requireField(safePayload.nucleoPatient(), "acolhimento.nucleoPatient", correlationId);
        return safePayload;
    }
}
