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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.humanizar.acolhimento.application.inbound.mapper.InboundEnvelopeMapper;
import com.humanizar.acolhimento.application.service.AcolhimentoDeleteService;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.controller.handler.AcolhimentoExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AcolhimentoDeleteControllerTest {

    @Mock
    private AcolhimentoDeleteService acolhimentoDeleteService;

    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        AcolhimentoDeleteController controller = new AcolhimentoDeleteController(
                acolhimentoDeleteService, new InboundEnvelopeMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AcolhimentoExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200WithSuccessBody() throws Exception {
        UUID patientId = UUID.fromString("544f31af-0a2e-4a6f-bfff-6c163f23d71a");

        mockMvc.perform(delete("/api/v1/acolhimento/delete/{patientId}", patientId)
                .contentType("application/json")
                .content(validDeleteEnvelope(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.operation").value("DELETE"))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));

        verify(acolhimentoDeleteService).deleteByPatientId(eq(patientId), any());
    }

    @Test
    void shouldReturn404WhenPatientDoesNotExist() throws Exception {
        UUID patientId = UUID.fromString("d865034d-4e8c-412f-8e06-a724d7ecf6e5");
        String correlationId = "corr-delete-404";

        doThrow(new AcolhimentoException(
                ReasonCode.PATIENT_NOT_FOUND,
                correlationId,
                "Paciente nao encontrado"))
                .when(acolhimentoDeleteService)
                .deleteByPatientId(eq(patientId), any());

        mockMvc.perform(delete("/api/v1/acolhimento/delete/{patientId}", patientId)
                .contentType("application/json")
                .content(validDeleteEnvelope(patientId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.reasonCode").value("PATIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.path").value("/api/v1/acolhimento/delete/" + patientId));

        verify(acolhimentoDeleteService).deleteByPatientId(eq(patientId), any());
    }

    @Test
    void shouldReturn409WhenDeleteIsAlreadyInProgress() throws Exception {
        UUID patientId = UUID.fromString("3e9b0d9d-1a5b-4a86-b0dd-e2d90f505f35");
        String correlationId = "corr-delete-409";

        doThrow(new AcolhimentoException(
                ReasonCode.DELETE_IN_PROGRESS,
                correlationId,
                "Ja existe operacao DELETE pendente para patientId=" + patientId))
                .when(acolhimentoDeleteService)
                .deleteByPatientId(eq(patientId), any());

        mockMvc.perform(delete("/api/v1/acolhimento/delete/{patientId}", patientId)
                .contentType("application/json")
                .content(validDeleteEnvelope(patientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.reasonCode").value("DELETE_IN_PROGRESS"))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.path").value("/api/v1/acolhimento/delete/" + patientId));

        verify(acolhimentoDeleteService).deleteByPatientId(eq(patientId), any());
    }

    private String validDeleteEnvelope(UUID patientId) {
        return """
                {
                  "correlationId": "f5e56969-d6c4-4b89-8ca8-403b6d3a0a20",
                  "producerService": "humanizar-admin-dashboard",
                  "occurredAt": "2026-03-02T14:40:00",
                  "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
                  "userAgent": "Mozilla/5.0",
                  "originIp": "127.0.0.1",
                  "payload": {
                    "patientId": "%s"
                  }
                }
                """.formatted(patientId);
    }
}
