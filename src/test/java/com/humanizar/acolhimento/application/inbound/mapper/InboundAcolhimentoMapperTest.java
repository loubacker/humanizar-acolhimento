package com.humanizar.acolhimento.application.inbound.mapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoMappingResult;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class InboundAcolhimentoMapperTest {

        private final InboundAcolhimentoMapper mapper = new InboundAcolhimentoMapper(new NucleoPatientInboundMapper());
        private static final String CORRELATION_ID = "corr-123";

        @Test
        void shouldMapCreateWithGeneratedIdsForAllNucleos() {
                UUID acolhimentoId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                UUID nucleoA = UUID.randomUUID();
                UUID nucleoB = UUID.randomUUID();
                InboundAcolhimentoDTO payload = payload(patientId, nucleoA, nucleoB);

                InboundAcolhimentoMappingResult result = mapper.mapCreate(payload, acolhimentoId, CORRELATION_ID);

                assertEquals(acolhimentoId, result.acolhimento().getId());
                assertEquals(patientId, result.acolhimento().getPatientId());
                assertNotNull(result.acolhimento().getCreatedAt());
                assertNotNull(result.acolhimento().getUpdatedAt());
                assertEquals(2, result.nucleoPatientIdsByNucleoId().size());
                assertTrue(result.nucleoPatientIdsByNucleoId().containsKey(nucleoA));
                assertTrue(result.nucleoPatientIdsByNucleoId().containsKey(nucleoB));
                assertEquals(2, result.nucleoPatients().size());
                assertEquals(3, result.nucleoPatientResponsaveis().size());
        }

        @Test
        void shouldMapUpdateReusingExistingAndGeneratingOnlyForNewNucleo() {
                UUID acolhimentoId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                UUID existingNucleoId = UUID.randomUUID();
                UUID newNucleoId = UUID.randomUUID();
                UUID existingNucleoPatientId = UUID.randomUUID();
                InboundAcolhimentoDTO payload = payload(patientId, existingNucleoId, newNucleoId);

                InboundAcolhimentoMappingResult result = mapper.mapUpdate(
                                payload,
                                acolhimentoId,
                                Map.of(existingNucleoId, existingNucleoPatientId),
                                CORRELATION_ID);

                assertEquals(acolhimentoId, result.acolhimento().getId());
                assertEquals(existingNucleoPatientId, result.nucleoPatientIdsByNucleoId().get(existingNucleoId));
                assertTrue(result.nucleoPatientIdsByNucleoId().containsKey(newNucleoId));
                assertNotEquals(existingNucleoPatientId, result.nucleoPatientIdsByNucleoId().get(newNucleoId));
                assertEquals(2, result.nucleoPatients().size());
        }

        @Test
        void shouldFailWhenPayloadIsMissingRequiredPatientId() {
                InboundAcolhimentoDTO payload = new InboundAcolhimentoDTO(
                                null,
                                LocalDate.now(),
                                LocalTime.NOON,
                                "obs",
                                List.of(new NucleoPatientDTO(
                                                UUID.randomUUID(),
                                                List.of(new NucleoResponsavelDTO(UUID.randomUUID(), "COORDENADOR")))));

                AcolhimentoException exception = assertThrows(
                                AcolhimentoException.class,
                                () -> mapper.mapCreate(payload, UUID.randomUUID(), CORRELATION_ID));

                assertEquals(ReasonCode.INBOUND_REQUIRED_FIELD, exception.getReasonCode());
        }

        private InboundAcolhimentoDTO payload(UUID patientId, UUID nucleoA, UUID nucleoB) {
                return new InboundAcolhimentoDTO(
                                patientId,
                                LocalDate.now(),
                                LocalTime.of(10, 30),
                                "observacao",
                                List.of(
                                                new NucleoPatientDTO(
                                                                nucleoA,
                                                                List.of(
                                                                                new NucleoResponsavelDTO(
                                                                                                UUID.randomUUID(),
                                                                                                "COORDENADOR"),
                                                                                new NucleoResponsavelDTO(
                                                                                                UUID.randomUUID(),
                                                                                                "ADMINISTRADOR"))),
                                                new NucleoPatientDTO(
                                                                nucleoB,
                                                                List.of(new NucleoResponsavelDTO(UUID.randomUUID(),
                                                                                "COORDENADOR")))));
        }
}
