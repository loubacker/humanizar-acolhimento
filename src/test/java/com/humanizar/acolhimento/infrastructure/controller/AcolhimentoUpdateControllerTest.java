package com.humanizar.acolhimento.infrastructure.controller;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.humanizar.acolhimento.application.inbound.mapper.InboundEnvelopeMapper;
import com.humanizar.acolhimento.application.service.AcolhimentoUpdateService;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoUpdateResponseDTO;
import com.humanizar.acolhimento.infrastructure.controller.handler.AcolhimentoExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AcolhimentoUpdateControllerTest {

  @Mock
  private AcolhimentoUpdateService acolhimentoUpdateService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AcolhimentoUpdateController controller = new AcolhimentoUpdateController(
        acolhimentoUpdateService, new InboundEnvelopeMapper());
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new AcolhimentoExceptionHandler())
        .build();
  }

  @Test
  void shouldReturn200OnUpdate() throws Exception {
    UUID patientId = UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb");
    UUID correlationId = UUID.fromString("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20");

    when(acolhimentoUpdateService.updateByPatientId(eq(patientId), any()))
        .thenReturn(new AcolhimentoUpdateResponseDTO(
            "Acolhimento Atualizado com Sucesso para Paciente " + patientId,
            patientId,
            correlationId));

    mockMvc.perform(put("/api/v1/acolhimento/update/{patientId}", patientId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(validEnvelopeBody(patientId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Acolhimento Atualizado com Sucesso para Paciente " + patientId))
        .andExpect(jsonPath("$.patientId").value(patientId.toString()))
        .andExpect(jsonPath("$.correlationId").value(correlationId.toString()));

    verify(acolhimentoUpdateService).updateByPatientId(eq(patientId), any());
  }

  @Test
  void shouldReturn400WhenPathPayloadPatientIdMismatch() throws Exception {
    UUID patientId = UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb");
    String correlationId = "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20";

    doThrow(new AcolhimentoException(
        ReasonCode.INBOUND_PATIENT_MISMATCH,
        correlationId,
        "path.patientId diverge de payload.patientId"))
        .when(acolhimentoUpdateService)
        .updateByPatientId(eq(patientId), any());

    mockMvc.perform(put("/api/v1/acolhimento/update/{patientId}", patientId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(validEnvelopeBody(patientId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.reasonCode").value("INBOUND_PATIENT_MISMATCH"))
        .andExpect(jsonPath("$.correlationId").value(correlationId))
        .andExpect(jsonPath("$.path").value("/api/v1/acolhimento/update/" + patientId));

    verify(acolhimentoUpdateService).updateByPatientId(eq(patientId), any());
  }

  private String validEnvelopeBody(UUID patientId) {
    return """
        {
          "correlationId":"f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
          "producerService":"humanizar-admin-dashboard",
          "occurredAt":"2026-03-02T14:00:00",
          "actorId":"f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
          "userAgent":"Mozilla/5.0",
          "originIp":"127.0.0.1",
          "payload":{
            "patientId":"%s",
            "dataAnamnese":"2026-03-02",
            "horaAnamnese":"11:35:00",
            "observacoes":"Paciente atualizado.",
            "nucleoPatient":[
              {
                "nucleoId":"c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc",
                "nucleoPatientResponsavel":[
                  {
                    "responsavelId":"d0f0acb9-2def-4c2b-9a97-5f2d705c4cbe",
                    "role":"COORDENADOR"
                  }
                ]
              }
            ]
          }
        }
        """.formatted(patientId);
  }
}
