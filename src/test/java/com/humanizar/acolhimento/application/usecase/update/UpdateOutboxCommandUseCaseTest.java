package com.humanizar.acolhimento.application.usecase.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoMappingResult;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.outbound.mapper.OutboundUpdateMapper;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.ResponsavelRole;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@ExtendWith(MockitoExtension.class)
class UpdateOutboxCommandUseCaseTest {

        @Mock
        private OutboxEventPort outboxEventPort;

        private final OutboundUpdateMapper updateOutboundMapper = new OutboundUpdateMapper();
        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        @Captor
        private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

        private UpdateOutboxCommandUseCase useCase;

        @BeforeEach
        @SuppressWarnings("unused")
        void setUp() {
                useCase = new UpdateOutboxCommandUseCase(
                                outboxEventPort,
                                updateOutboundMapper,
                                objectMapper);
        }

        @Test
        void shouldPersistOutboxUpdateWithEdaMetadataFromEnvelope() throws Exception {
                UUID eventId = UUID.randomUUID();
                UUID correlationId = UUID.randomUUID();
                UUID aggregateId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                UUID nucleoId = UUID.randomUUID();
                UUID nucleoPatientId = UUID.randomUUID();
                UUID actorId = UUID.randomUUID();
                UUID responsavelId = UUID.randomUUID();

                InboundEnvelopeDTO<InboundAcolhimentoDTO> inboundEnvelope = new InboundEnvelopeDTO<>(
                                correlationId,
                                "humanizar-admin-dashboard",
                                LocalDateTime.now(),
                                actorId,
                                "JUnit",
                                "127.0.0.1",
                                new InboundAcolhimentoDTO(
                                                patientId,
                                                LocalDate.now(),
                                                LocalTime.of(10, 30),
                                                "obs update",
                                                List.of(new NucleoPatientDTO(
                                                                nucleoId,
                                                                List.of(new NucleoResponsavelDTO(responsavelId,
                                                                                "COORDENADOR"))))));

                InboundAcolhimentoMappingResult mappingResult = new InboundAcolhimentoMappingResult(
                                Acolhimento.builder()
                                                .id(aggregateId)
                                                .patientId(patientId)
                                                .dataAnamnese(LocalDate.now())
                                                .horaAnamnese(LocalTime.of(10, 30))
                                                .observacoes("obs update")
                                                .createdAt(LocalDateTime.now().minusDays(1))
                                                .updatedAt(LocalDateTime.now())
                                                .build(),
                                List.of(NucleoPatient.builder()
                                                .id(nucleoPatientId)
                                                .patientId(patientId)
                                                .nucleoId(nucleoId)
                                                .nucleoPatientResponsavel(List.of())
                                                .build()),
                                List.of(NucleoPatientResponsavel.builder()
                                                .id(UUID.randomUUID())
                                                .nucleoPatientId(nucleoPatientId)
                                                .responsavelId(responsavelId)
                                                .role(ResponsavelRole.COORDENADOR)
                                                .build()),
                                Map.of(nucleoId, nucleoPatientId));

                when(outboxEventPort.save(org.mockito.ArgumentMatchers.any(OutboxEvent.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                useCase.execute(inboundEnvelope, eventId, mappingResult);

                verify(outboxEventPort).save(outboxEventCaptor.capture());
                OutboxEvent saved = outboxEventCaptor.getValue();

                assertEquals(eventId, saved.getEventId());
                assertEquals(correlationId, saved.getCorrelationId());
                assertEquals(actorId, saved.getActorId());
                assertEquals("JUnit", saved.getUserAgent());
                assertEquals("127.0.0.1", saved.getOriginIp());
                assertEquals("cmd.acolhimento.updated.v1", saved.getRoutingKey());
                assertEquals(OutboxStatus.NEW, saved.getStatus());
                assertNotNull(saved.getPayload());

                JsonNode payloadJson = objectMapper.readTree(saved.getPayload());
                assertEquals(correlationId.toString(), payloadJson.get("correlationId").asText());
                assertEquals(actorId.toString(), payloadJson.get("actorId").asText());
                assertEquals("humanizar.acolhimento.command", payloadJson.get("exchangeName").asText());
                assertEquals("cmd.acolhimento.updated.v1", payloadJson.get("routingKey").asText());
                assertEquals(patientId.toString(), payloadJson.get("payload").get("patientId").asText());
        }
}
