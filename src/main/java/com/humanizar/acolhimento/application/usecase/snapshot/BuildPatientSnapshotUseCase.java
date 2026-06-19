package com.humanizar.acolhimento.application.usecase.snapshot;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.http.dto.anamnese.AcolhimentoListResponseDTO;
import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.anamnese.NucleoComAnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.http.mapper.AnamneseMapper;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.application.usecase.anamnese.RetrieveAnamneseUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;

@Service
public class BuildPatientSnapshotUseCase {

    private final RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase;
    private final RetrieveNucleoPatientResponsavelUseCase retrieveNucleoPatientResponsavelUseCase;
    private final RetrieveAnamneseUseCase retrieveAnamneseUseCase;
    private final AnamneseMapper anamneseMapper;
    private final JsonPayloadSerializer jsonPayloadSerializer;

    public BuildPatientSnapshotUseCase(
            RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase,
            RetrieveNucleoPatientResponsavelUseCase retrieveNucleoPatientResponsavelUseCase,
            RetrieveAnamneseUseCase retrieveAnamneseUseCase,
            AnamneseMapper anamneseMapper,
            JsonPayloadSerializer jsonPayloadSerializer) {
        this.retrieveNucleoPatientUseCase = retrieveNucleoPatientUseCase;
        this.retrieveNucleoPatientResponsavelUseCase = retrieveNucleoPatientResponsavelUseCase;
        this.retrieveAnamneseUseCase = retrieveAnamneseUseCase;
        this.anamneseMapper = anamneseMapper;
        this.jsonPayloadSerializer = jsonPayloadSerializer;
    }

    public AcolhimentoListResponseDTO execute(UUID patientId) {
        List<NucleoPatient> nucleoPatients = retrieveNucleoPatientUseCase.execute(patientId);

        Map<UUID, AnamneseDTO> anamnesesByNucleoPatientId = loadAnamneses(nucleoPatients);

        List<NucleoComAnamneseDTO> nucleos = nucleoPatients.stream()
                .map(nucleoPatient -> new NucleoComAnamneseDTO(
                        nucleoPatient.getNucleoId(),
                        toResponsaveis(nucleoPatient.getId()),
                        anamnesesByNucleoPatientId.get(nucleoPatient.getId()),
                        nucleoPatient.getAnamneseStatus() != null
                                ? nucleoPatient.getAnamneseStatus().name()
                                : null))
                .toList();

        return new AcolhimentoListResponseDTO(patientId, nucleos);
    }

    public String serialize(UUID patientId, String correlationId) {
        return jsonPayloadSerializer.serialize(
                execute(patientId),
                correlationId,
                "Falha ao serializar payloadSnapshot de pending_acolhimento");
    }

    private Map<UUID, AnamneseDTO> loadAnamneses(List<NucleoPatient> nucleoPatients) {
        List<UUID> nucleoPatientIds = nucleoPatients.stream()
                .map(NucleoPatient::getId)
                .toList();

        return retrieveAnamneseUseCase.executeBatch(nucleoPatientIds).stream()
                .collect(Collectors.toMap(
                        Acolhimento::getNucleoPatientId,
                        anamneseMapper::toDTO,
                        (existing, replacement) -> existing));
    }

    private List<NucleoResponsavelDTO> toResponsaveis(UUID nucleoPatientId) {
        return retrieveNucleoPatientResponsavelUseCase.execute(nucleoPatientId).stream()
                .map(responsavel -> new NucleoResponsavelDTO(
                        responsavel.getResponsavelId(),
                        responsavel.getRole() != null ? responsavel.getRole().name() : null))
                .toList();
    }
}
