package com.humanizar.acolhimento.application.http.mapper;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;

@Component
public class AnamneseMapper {

    private final InboundFieldValidator fieldValidator;

    public AnamneseMapper(InboundFieldValidator fieldValidator) {
        this.fieldValidator = fieldValidator;
    }

    public AnamneseDTO toAnamnesePayload(AnamneseDTO anamneseDTO, String correlationId) {
        AnamneseDTO safePayload = fieldValidator.requireField(anamneseDTO, "anamnese", correlationId);
        fieldValidator.requireField(safePayload.dataAnamnese(), "anamnese.dataAnamnese", correlationId);
        fieldValidator.requireField(safePayload.horaAnamnese(), "anamnese.horaAnamnese", correlationId);
        return safePayload;
    }

    public Acolhimento toDomain(AnamneseDTO anamneseDTO, String correlationId) {
        AnamneseDTO safePayload = toAnamnesePayload(anamneseDTO, correlationId);
        return Acolhimento.builder()
                .dataAnamnese(safePayload.dataAnamnese())
                .horaAnamnese(safePayload.horaAnamnese())
                .observacoes(safePayload.observacoes())
                .build();
    }

    public AnamneseDTO toDTO(Acolhimento acolhimento) {
        if (acolhimento == null) {
            return null;
        }
        return new AnamneseDTO(
                acolhimento.getDataAnamnese(),
                acolhimento.getHoraAnamnese(),
                acolhimento.getObservacoes());
    }
}
