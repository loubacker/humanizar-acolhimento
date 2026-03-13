package com.humanizar.acolhimento.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDTO;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundNucleoResponsavelDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundNucleoPatientDTO;
import com.humanizar.acolhimento.infrastructure.config.ObjectMapperConfig;

class OutboundEnvelopeDTOSerializationTest {

        private ObjectMapper objectMapper;

        @BeforeEach
        @SuppressWarnings("unused")
        void setUp() {
                objectMapper = new ObjectMapperConfig().objectMapper();
        }

        @Test
        void shouldSerializeCreatedOutboundEnvelope() throws Exception {
                OutboundEnvelopeDTO<AcolhimentoCommandDTO> envelope = new OutboundEnvelopeDTO<>(
                                UUID.fromString("7b736ee4-7fc4-48dc-ae60-01050f660f9f"),
                                UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"),
                                "humanizar-acolhimento",
                                "humanizar.acolhimento.command",
                                "cmd.acolhimento.created.v1",
                                "acolhimento",
                                UUID.fromString("8206e539-fc43-42f8-8bd5-bf6164b835dc"),
                                1,
                                LocalDateTime.of(2026, 2, 13, 21, 0),
                                UUID.fromString("f8b9bf6f-39df-4ba8-b95e-5e5e526a725f"),
                                "Mozilla/5.0",
                                "187.11.22.33",
                                new AcolhimentoCommandDTO(
                                                UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"),
                                                List.of(new OutboundNucleoPatientDTO(
                                                                UUID.fromString("2e06688f-481f-4ccc-9d32-b2132ca83112"),
                                                                UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"),
                                                                UUID.fromString("c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc"),
                                                                List.of(new OutboundNucleoResponsavelDTO(
                                                                                UUID.fromString("2e06688f-481f-4ccc-9d32-b2132ca83112"),
                                                                                UUID.fromString("d0f0acb9-2def-4c2b-9a97-5f2d705c4cbe"),
                                                                                "COORDENADOR"))))));

                JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(envelope));

                assertEquals("cmd.acolhimento.created.v1", json.get("routingKey").asText());
                assertTrue(json.get("payload").has("nucleoPatient"));
                assertTrue(json.get("payload").get("nucleoPatient").get(0).has("nucleoPatientId"));
                assertTrue(json.get("payload").get("nucleoPatient").get(0).has("nucleoPatientResponsavel"));
                assertFalse(json.get("payload").get("nucleoPatient").get(0).has("outbounNucleoResponsavelDTOs"));
        }

        @Test
        void shouldSerializeUpdatedOutboundEnvelope() throws Exception {
                OutboundEnvelopeDTO<AcolhimentoCommandDTO> envelope = new OutboundEnvelopeDTO<>(
                                UUID.fromString("8e976f4d-92da-4cd6-a636-b66f3e4a4f3e"),
                                UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"),
                                "humanizar-acolhimento",
                                "humanizar.acolhimento.command",
                                "cmd.acolhimento.updated.v1",
                                "acolhimento",
                                UUID.fromString("8206e539-fc43-42f8-8bd5-bf6164b835dc"),
                                1,
                                LocalDateTime.of(2026, 2, 13, 21, 20),
                                UUID.fromString("f8b9bf6f-39df-4ba8-b95e-5e5e526a725f"),
                                "Mozilla/5.0",
                                "187.11.22.33",
                                new AcolhimentoCommandDTO(
                                                UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"),
                                                List.of(new OutboundNucleoPatientDTO(
                                                                UUID.fromString("2e06688f-481f-4ccc-9d32-b2132ca83112"),
                                                                UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"),
                                                                UUID.fromString("c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc"),
                                                                List.of(new OutboundNucleoResponsavelDTO(
                                                                                UUID.fromString("2e06688f-481f-4ccc-9d32-b2132ca83112"),
                                                                                UUID.fromString("bb3e9f59-1a4c-4aca-9e66-5e7c1a0c8b6f"),
                                                                                "ADMINISTRADOR"))))));

                JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(envelope));

                assertEquals("cmd.acolhimento.updated.v1", json.get("routingKey").asText());
                assertEquals("ADMINISTRADOR",
                                json.get("payload")
                                                .get("nucleoPatient")
                                                .get(0)
                                                .get("nucleoPatientResponsavel")
                                                .get(0)
                                                .get("role")
                                                .asText());
        }

        @Test
        void shouldSerializeDeletedOutboundEnvelope() throws Exception {
                OutboundEnvelopeDTO<AcolhimentoCommandDeletedDTO> envelope = new OutboundEnvelopeDTO<>(
                                UUID.fromString("f2d68531-9cdf-4af5-aec9-af919d64f9f7"),
                                UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"),
                                "humanizar-acolhimento",
                                "humanizar.acolhimento.command",
                                "cmd.acolhimento.deleted.v1",
                                "acolhimento",
                                UUID.fromString("8206e539-fc43-42f8-8bd5-bf6164b835dc"),
                                1,
                                LocalDateTime.of(2026, 2, 13, 21, 40),
                                UUID.fromString("f8b9bf6f-39df-4ba8-b95e-5e5e526a725f"),
                                "Mozilla/5.0",
                                "187.11.22.33",
                                new AcolhimentoCommandDeletedDTO(
                                                UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb")));

                JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(envelope));

                assertEquals("cmd.acolhimento.deleted.v1", json.get("routingKey").asText());
                assertEquals("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb", json.get("payload").get("patientId").asText());
        }
}
