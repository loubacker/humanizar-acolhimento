package com.humanizar.acolhimento.application.outbound.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoMappingResult;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundNucleoResponsavelDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundNucleoPatientDTO;

@Component
public class OutboundUpsertMapper {

    private static final String PRODUCER_SERVICE = "humanizar-acolhimento";
    private static final String AGGREGATE_TYPE = "acolhimento";
    private static final short EVENT_VERSION = 1;

    public OutboundEnvelopeDTO<AcolhimentoCommandDTO> toCommandEnvelope(
            InboundEnvelopeDTO<InboundAcolhimentoDTO> inboundEnvelope,
            UUID eventId,
            InboundAcolhimentoMappingResult mappingResult,
            String routingKey) {
        Objects.requireNonNull(inboundEnvelope, "inboundEnvelope é obrigatório");
        Objects.requireNonNull(mappingResult, "mappingResult é obrigatório");
        Objects.requireNonNull(mappingResult.acolhimento(), "mappingResult.acolhimento é obrigatório");
        Objects.requireNonNull(mappingResult.acolhimento().getId(),
                "mappingResult.acolhimento.id é obrigatório");
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
                mappingResult.acolhimento().getPatientId(),
                nucleoPatient);

        return new OutboundEnvelopeDTO<>(
                eventId,
                inboundEnvelope.correlationId(),
                PRODUCER_SERVICE,
                ExchangeCatalog.ACOLHIMENTO_COMMAND,
                routingKey,
                AGGREGATE_TYPE,
                mappingResult.acolhimento().getId(),
                EVENT_VERSION,
                LocalDateTime.now(),
                inboundEnvelope.actorId(),
                inboundEnvelope.userAgent(),
                inboundEnvelope.originIp(),
                payload);
    }
}
