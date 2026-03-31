package com.humanizar.acolhimento.infrastructure.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.humanizar.acolhimento.application.inbound.mapper.InboundEnvelopeMapper;
import com.humanizar.acolhimento.application.service.AcolhimentoCreateService;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.controller.handler.AcolhimentoExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AcolhimentoCreateControllerTest {

  @Mock
  private AcolhimentoCreateService acolhimentoCreateService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AcolhimentoCreateController controller = new AcolhimentoCreateController(
        acolhimentoCreateService, new InboundEnvelopeMapper());
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new AcolhimentoExceptionHandler())
        .build();
  }

  @Test
  void shouldReturn200OnRegister() throws Exception {
    String body = """
        {
          "correlationId":"f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
          "producerService":"humanizar-gateway",
          "occurredAt":"2026-03-02T14:00:00",
          "actorId":"f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
          "userAgent":"Mozilla/5.0",
          "originIp":"187.11.22.33",
          "payload":{
            "patientId":"0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb",
            "dataAnamnese":"2026-03-02",
            "horaAnamnese":"11:35:00",
            "observacoes":"Paciente chegou acompanhado.",
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
        """;

    mockMvc.perform(post("/api/v1/acolhimento/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(acolhimentoCreateService).register(any());
  }

  @Test
  void shouldReturn409WithReasonCodeOnDuplicate() throws Exception {
    String body = """
        {
          "correlationId":"f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
          "producerService":"humanizar-gateway",
          "occurredAt":"2026-03-02T14:00:00",
          "actorId":"f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
          "userAgent":"Mozilla/5.0",
          "originIp":"187.11.22.33",
          "payload":{
            "patientId":"0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb",
            "dataAnamnese":"2026-03-02",
            "horaAnamnese":"11:35:00",
            "observacoes":"Paciente chegou acompanhado.",
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
        """;

    doThrow(new AcolhimentoException(
        ReasonCode.DUPLICATE_PATIENT,
        "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
        "Acolhimento ja existe"))
        .when(acolhimentoCreateService)
        .register(any());

    mockMvc.perform(post("/api/v1/acolhimento/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.reasonCode").value("DUPLICATE_PATIENT"))
        .andExpect(jsonPath("$.correlationId").value("f5e56969-d6c4-4b89-8ca8-403b6d3a0a20"))
        .andExpect(jsonPath("$.path").value("/api/v1/acolhimento/register"));

    verify(acolhimentoCreateService).register(any());
  }
}
