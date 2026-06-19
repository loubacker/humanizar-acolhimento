package com.humanizar.acolhimento.application.http.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.http.dto.acolhimento.InboundAcolhimentoMapResultDTO;
import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.http.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;

class InboundAcolhimentoMapperTest {

        private final InboundFieldValidator fieldValidator = new InboundFieldValidator();
        private final InboundAcolhimentoMapper mapper = new InboundAcolhimentoMapper(
                        new NucleoPatientInboundMapper(fieldValidator), fieldValidator);
        private static final String CORRELATION_ID = "corr-123";

        @Test
        void shouldMapCreateWithGeneratedIdsForAllNucleos() {
                UUID patientId = UUID.randomUUID();
                UUID nucleoA = UUID.randomUUID();
                UUID nucleoB = UUID.randomUUID();
                InboundAcolhimentoDTO payload = payload(patientId, nucleoA, nucleoB);

                InboundAcolhimentoMapResultDTO result = mapper.mapCreate(payload, CORRELATION_ID);

                assertEquals(patientId, result.patientId());
                assertEquals(2, result.nucleoPatientIdsByNucleoId().size());
                assertTrue(result.nucleoPatientIdsByNucleoId().containsKey(nucleoA));
                assertTrue(result.nucleoPatientIdsByNucleoId().containsKey(nucleoB));
                assertEquals(2, result.nucleoPatients().size());
                assertEquals(3, result.nucleoPatientResponsaveis().size());
        }

        @Test
        void shouldMapUpdateReusingExistingAndGeneratingOnlyForNewNucleo() {
                UUID patientId = UUID.randomUUID();
                UUID existingNucleoId = UUID.randomUUID();
                UUID newNucleoId = UUID.randomUUID();
                UUID existingNucleoPatientId = UUID.randomUUID();
                InboundAcolhimentoDTO payload = payload(patientId, existingNucleoId, newNucleoId);

                InboundAcolhimentoMapResultDTO result = mapper.mapUpdate(
                                payload,
                                Map.of(existingNucleoId, existingNucleoPatientId),
                                Map.of(existingNucleoId, AnamneseStatus.ENTREGUE),
                                CORRELATION_ID);

                assertEquals(patientId, result.patientId());
                assertEquals(existingNucleoPatientId, result.nucleoPatientIdsByNucleoId().get(existingNucleoId));
                assertTrue(result.nucleoPatientIdsByNucleoId().containsKey(newNucleoId));
                assertNotEquals(existingNucleoPatientId, result.nucleoPatientIdsByNucleoId().get(newNucleoId));
                assertEquals(2, result.nucleoPatients().size());

                AnamneseStatus statusExistente = result.nucleoPatients().stream()
                                .filter(nucleo -> nucleo.getNucleoId().equals(existingNucleoId))
                                .findFirst().orElseThrow().getAnamneseStatus();
                AnamneseStatus statusNovo = result.nucleoPatients().stream()
                                .filter(nucleo -> nucleo.getNucleoId().equals(newNucleoId))
                                .findFirst().orElseThrow().getAnamneseStatus();
                assertEquals(AnamneseStatus.ENTREGUE, statusExistente);
                assertEquals(AnamneseStatus.PENDENTE, statusNovo);
        }

        @Test
        void shouldFailWhenPayloadIsMissingRequiredPatientId() {
                InboundAcolhimentoDTO payload = new InboundAcolhimentoDTO(
                                null,
                                List.of(new NucleoPatientDTO(
                                                UUID.randomUUID(),
                                                List.of(new NucleoResponsavelDTO(UUID.randomUUID(), "COORDENADOR")))));

                AcolhimentoException exception = assertThrows(
                                AcolhimentoException.class,
                                () -> mapper.mapCreate(payload, CORRELATION_ID));

                assertEquals(ReasonCode.INBOUND_REQUIRED_FIELD, exception.getReasonCode());
        }

        private InboundAcolhimentoDTO payload(UUID patientId, UUID nucleoA, UUID nucleoB) {
                return new InboundAcolhimentoDTO(
                                patientId,
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
