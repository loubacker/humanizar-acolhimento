package com.humanizar.acolhimento.application.messaging.outbound.mapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoMapResultDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.AcolhimentoCommandDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundNucleoResponsavelDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.messaging.outbound.dto.OutboundNucleoPatientDTO;
import com.humanizar.acolhimento.application.messaging.outbound.factory.OutboundEnvelopeFactory;

@Component
public class OutboundUpsertMapper {

    private final OutboundEnvelopeFactory outboundEnvelopeFactory;

    public OutboundUpsertMapper(OutboundEnvelopeFactory outboundEnvelopeFactory) {
        this.outboundEnvelopeFactory = outboundEnvelopeFactory;
    }

    public OutboundEnvelopeDTO<AcolhimentoCommandDTO> toCommandEnvelope(
            HttpEnvelopeDTO<InboundAcolhimentoDTO> inboundEnvelope,
            UUID eventId,
            InboundAcolhimentoMapResultDTO mappingResult,
            String routingKey) {
        Objects.requireNonNull(inboundEnvelope, "inboundEnvelope é obrigatório");
        Objects.requireNonNull(mappingResult, "mappingResult é obrigatório");
        Objects.requireNonNull(mappingResult.patientId(), "mappingResult.patientId é obrigatório");
        Objects.requireNonNull(eventId, "eventId é obrigatório");
        Objects.requireNonNull(routingKey, "routingKey é obrigatório");

        Map<UUID, List<OutboundNucleoResponsavelDTO>> responsaveisByNucleoPatientId = mappingResult
                .nucleoPatientResponsaveis().stream()
                .collect(Collectors.groupingBy(
                        NucleoPatientResponsavel::getNucleoPatientId,
                        Collectors.mapping(
                                responsavel -> new OutboundNucleoResponsavelDTO(
                                        responsavel.getNucleoPatientId(),
                                        responsavel.getResponsavelId(),
                                        responsavel.getRole() != null
                                                ? responsavel.getRole()
                                                        .name()
                                                : null),
                                Collectors.toList())));

        List<OutboundNucleoPatientDTO> nucleoPatient = mappingResult.nucleoPatients().stream()
                .map(nucleo -> new OutboundNucleoPatientDTO(
                        nucleo.getId(),
                        nucleo.getPatientId(),
                        nucleo.getNucleoId(),
                        responsaveisByNucleoPatientId.getOrDefault(nucleo.getId(), List.of())))
                .toList();

        AcolhimentoCommandDTO payload = new AcolhimentoCommandDTO(
                mappingResult.patientId(),
                nucleoPatient);

        return outboundEnvelopeFactory.command(
                inboundEnvelope,
                eventId,
                routingKey,
                mappingResult.patientId(),
                payload);
    }
}
