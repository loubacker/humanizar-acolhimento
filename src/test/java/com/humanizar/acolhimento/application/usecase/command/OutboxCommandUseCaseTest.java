package com.humanizar.acolhimento.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoMapResultDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.messaging.outbound.factory.CommandOutboxEventFactory;
import com.humanizar.acolhimento.application.messaging.outbound.factory.OutboundEnvelopeFactory;
import com.humanizar.acolhimento.application.messaging.outbound.mapper.OutboundUpsertMapper;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.domain.model.OutboxEvent;
import com.humanizar.acolhimento.domain.model.enums.OutboxStatus;
import com.humanizar.acolhimento.domain.model.enums.ResponsavelRole;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;
import com.humanizar.acolhimento.domain.port.OutboxEventPort;

@ExtendWith(MockitoExtension.class)
class OutboxCommandUseCaseTest {

        @Mock
        private OutboxEventPort outboxEventPort;

        private final OutboundUpsertMapper outboundUpsertMapper = new OutboundUpsertMapper(
                        new OutboundEnvelopeFactory());
        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        @Captor
        private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

        private OutboxCommandUseCase useCase;

        @BeforeEach
        void setUp() {
                useCase = new OutboxCommandUseCase(
                                outboxEventPort,
                                outboundUpsertMapper,
                                new JsonPayloadSerializer(objectMapper),
                                new CommandOutboxEventFactory());
        }

        @Test
        void shouldPersistOutboxCreateWithEdaMetadataFromEnvelope() throws Exception {
                assertOutboxPersistedFor(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
        }

        @Test
        void shouldPersistOutboxUpdateWithEdaMetadataFromEnvelope() throws Exception {
                assertOutboxPersistedFor(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_UPDATED_V1);
        }

        private void assertOutboxPersistedFor(String routingKey) throws Exception {
                UUID eventId = UUID.randomUUID();
                UUID correlationId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                UUID nucleoId = UUID.randomUUID();
                UUID nucleoPatientId = UUID.randomUUID();
                UUID actorId = UUID.randomUUID();
                UUID responsavelId = UUID.randomUUID();

                HttpEnvelopeDTO<InboundAcolhimentoDTO> inboundEnvelope = new HttpEnvelopeDTO<>(
                                correlationId,
                                "humanizar-admin-dashboard",
                                LocalDateTime.now(),
                                actorId,
                                "JUnit",
                                "127.0.0.1",
                                new InboundAcolhimentoDTO(
                                                patientId,
                                                List.of(new NucleoPatientDTO(
                                                                nucleoId,
                                                                List.of(new NucleoResponsavelDTO(responsavelId,
                                                                                "COORDENADOR"))))));

                InboundAcolhimentoMapResultDTO mappingResult = new InboundAcolhimentoMapResultDTO(
                                patientId,
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

                useCase.execute(inboundEnvelope, eventId, mappingResult, routingKey);

                verify(outboxEventPort).save(outboxEventCaptor.capture());
                OutboxEvent saved = outboxEventCaptor.getValue();

                assertEquals(eventId, saved.getEventId());
                assertEquals(correlationId, saved.getCorrelationId());
                assertEquals(actorId, saved.getActorId());
                assertEquals("JUnit", saved.getUserAgent());
                assertEquals("127.0.0.1", saved.getOriginIp());
                assertEquals(routingKey, saved.getRoutingKey());
                assertEquals(patientId, saved.getAggregateId());
                assertEquals(OutboxStatus.NEW, saved.getStatus());
                assertNotNull(saved.getPayload());

                JsonNode payloadJson = objectMapper.readTree(saved.getPayload());
                assertEquals(correlationId.toString(), payloadJson.get("correlationId").asText());
                assertEquals(actorId.toString(), payloadJson.get("actorId").asText());
                assertEquals("humanizar.acolhimento.command", payloadJson.get("exchangeName").asText());
                assertEquals(routingKey, payloadJson.get("routingKey").asText());
                assertEquals(patientId.toString(), payloadJson.get("aggregateId").asText());
                assertEquals(patientId.toString(), payloadJson.get("payload").get("patientId").asText());
        }
}
