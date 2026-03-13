package com.humanizar.acolhimento.application.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.infrastructure.config.ObjectMapperConfig;

class EnvelopeDTODeserializationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  @SuppressWarnings("unused")
  void setUp() {
    objectMapper = new ObjectMapperConfig().objectMapper();
  }

  @Test
  void shouldDeserializeCreateRequestedCommandEnvelope() throws Exception {
    InboundEnvelopeDTO<InboundAcolhimentoDTO> command = objectMapper.readValue(createJson(),
        new TypeReference<InboundEnvelopeDTO<InboundAcolhimentoDTO>>() {
        });

    assertEquals(UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"), command.correlationId());
    assertEquals("humanizar-gateway", command.producerService());
    assertEquals(LocalDateTime.of(2026, 3, 2, 14, 0, 0), command.occurredAt());
    assertNotNull(command.payload());
    assertEquals(UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"), command.payload().patientId());
    assertEquals(LocalDate.of(2026, 3, 2), command.payload().dataAnamnese());
    assertEquals(LocalTime.of(11, 35), command.payload().horaAnamnese());
    assertEquals(1, command.payload().nucleoPatient().size());
    assertEquals(UUID.fromString("c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc"),
        command.payload().nucleoPatient().getFirst().nucleoId());
    assertEquals(2, command.payload().nucleoPatient().getFirst().nucleoPatientResponsavel().size());
  }

  @Test
  void shouldDeserializeUpdateRequestedCommandEnvelope() throws Exception {
    InboundEnvelopeDTO<InboundAcolhimentoDTO> command = objectMapper.readValue(updateJson(),
        new TypeReference<InboundEnvelopeDTO<InboundAcolhimentoDTO>>() {
        });

    assertEquals(UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"), command.correlationId());
    assertEquals(LocalDate.of(2026, 3, 2), command.payload().dataAnamnese());
    assertEquals(LocalTime.of(11, 45), command.payload().horaAnamnese());
    assertEquals("Paciente com ajuste de observacao.", command.payload().observacoes());
    assertEquals(1, command.payload().nucleoPatient().size());
  }

  @Test
  void shouldDeserializeDeleteRequestedCommandEnvelope() throws Exception {
    InboundEnvelopeDTO<AcolhimentoDeleteDTO> command = objectMapper.readValue(deleteJson(),
        new TypeReference<InboundEnvelopeDTO<AcolhimentoDeleteDTO>>() {
        });

    assertEquals(UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"), command.correlationId());
    assertEquals(UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"), command.payload().patientId());
  }

  @Test
  void shouldIgnoreLegacyEnvelopeFieldsWhenPresent() throws Exception {
    InboundEnvelopeDTO<InboundAcolhimentoDTO> command = objectMapper.readValue(createJsonWithLegacyFields(),
        new TypeReference<InboundEnvelopeDTO<InboundAcolhimentoDTO>>() {
        });

    assertEquals(UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"), command.correlationId());
    assertEquals("humanizar-gateway", command.producerService());
    assertEquals(LocalDateTime.of(2026, 3, 2, 14, 0, 0), command.occurredAt());
    assertNotNull(command.payload());
    assertEquals(UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"), command.payload().patientId());
  }

  private String createJson() {
    return """
        {
          "correlationId": "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
          "producerService": "humanizar-gateway",
          "occurredAt": "2026-03-02T14:00:00",
          "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
          "userAgent": "Mozilla/5.0",
          "originIp": "187.11.22.33",
          "payload": {
            "patientId": "0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb",
            "dataAnamnese": "2026-03-02",
            "horaAnamnese": "11:35:00",
            "observacoes": "Paciente chegou acompanhado.",
            "nucleoPatient": [
              {
                "nucleoId": "c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc",
                "nucleoPatientResponsavel": [
                  {
                    "responsavelId": "d0f0acb9-2def-4c2b-9a97-5f2d705c4cbe",
                    "role": "COORDENADOR"
                  },
                  {
                    "responsavelId": "bb3e9f59-1a4c-4aca-9e66-5e7c1a0c8b6f",
                    "role": "ADMINISTRADOR"
                  }
                ]
              }
            ]
          }
        }
        """;
  }

  private String createJsonWithLegacyFields() {
    return """
        {
          "correlationId": "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
          "producerService": "humanizar-gateway",
          "exchangeName": "humanizar.acolhimento.command",
          "routingKey": "cmd.acolhimento.create.requested.v1",
          "eventVersion": 1,
          "occurredAt": "2026-03-02T14:00:00",
          "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
          "userAgent": "Mozilla/5.0",
          "originIp": "187.11.22.33",
          "payload": {
            "patientId": "0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb",
            "dataAnamnese": "2026-03-02",
            "horaAnamnese": "11:35:00",
            "observacoes": "Paciente chegou acompanhado.",
            "nucleoPatient": [
              {
                "nucleoId": "c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc",
                "nucleoPatientResponsavel": [
                  {
                    "responsavelId": "d0f0acb9-2def-4c2b-9a97-5f2d705c4cbe",
                    "role": "COORDENADOR"
                  }
                ]
              }
            ]
          }
        }
        """;
  }

  private String updateJson() {
    return """
        {
          "correlationId": "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
          "producerService": "humanizar-gateway",
          "occurredAt": "2026-03-02T14:25:00",
          "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
          "userAgent": "Mozilla/5.0",
          "originIp": "187.11.22.33",
          "payload": {
            "patientId": "0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb",
            "dataAnamnese": "2026-03-02",
            "horaAnamnese": "11:45:00",
            "observacoes": "Paciente com ajuste de observacao.",
            "nucleoPatient": [
              {
                "nucleoId": "c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc",
                "nucleoPatientResponsavel": [
                  {
                    "responsavelId": "d0f0acb9-2def-4c2b-9a97-5f2d705c4cbe",
                    "role": "COORDENADOR"
                  }
                ]
              }
            ]
          }
        }
        """;
  }

  private String deleteJson() {
    return """
        {
          "correlationId": "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
          "producerService": "humanizar-gateway",
          "occurredAt": "2026-03-02T14:40:00",
          "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
          "userAgent": "Mozilla/5.0",
          "originIp": "187.11.22.33",
          "payload": {
            "patientId": "0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb"
          }
        }
        """;
  }
}
