package com.humanizar.acolhimento.infrastructure.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoAnamneseService;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;
import com.humanizar.acolhimento.infrastructure.controller.handler.AcolhimentoExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AcolhimentoAnamneseControllerTest {

    private static final UUID PATIENT_ID = UUID.fromString("0f9c5a3a-6c8b-4f13-b1f4-9f3f0a98d9bb");
    private static final UUID NUCLEO_ID = UUID.fromString("c1a7f980-d466-4e5b-99ae-3d8c12b5d2cc");

    @Mock
    private AcolhimentoAnamneseService acolhimentoAnamneseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AcolhimentoAnamneseController controller = new AcolhimentoAnamneseController(acolhimentoAnamneseService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AcolhimentoExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn201OnCreate() throws Exception {
        String correlationId = "11111111-1111-1111-1111-111111111111";
        when(acolhimentoAnamneseService.create(eq(PATIENT_ID), eq(NUCLEO_ID), any()))
                .thenReturn(new SuccessResponseDTO(
                        "ANAMNESE_CREATED",
                        correlationId,
                        "Anamnese criada com sucesso",
                        new AnamneseDTO(LocalDate.of(2026, 3, 2), LocalTime.of(11, 35), "obs")));

        mockMvc.perform(post("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(envelopeBody(correlationId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-ID", correlationId))
                .andExpect(jsonPath("$.code").value("ANAMNESE_CREATED"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.message").value("Anamnese criada com sucesso"))
                .andExpect(jsonPath("$.data.dataAnamnese").value("2026-03-02"))
                .andExpect(jsonPath("$.data.horaAnamnese").value("11:35:00"))
                .andExpect(jsonPath("$.data.observacoes").value("obs"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(acolhimentoAnamneseService).create(eq(PATIENT_ID), eq(NUCLEO_ID), any());
    }

    @Test
    void shouldReturn404OnCreateWhenNucleoNotRegistered() throws Exception {
        doThrow(new AcolhimentoException(ReasonCode.NUCLEO_NOT_REGISTERED, "corr", "Nucleo nao cadastrado"))
                .when(acolhimentoAnamneseService).create(eq(PATIENT_ID), eq(NUCLEO_ID), any());

        mockMvc.perform(post("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(envelopeBody("11111111-1111-1111-1111-111111111111")))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", "corr"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.reasonCode").value("NUCLEO_NOT_REGISTERED"))
                .andExpect(jsonPath("$.message").value("Nucleo nao cadastrado"));
    }

    @Test
    void shouldReturn409OnCreateWhenAnamneseAlreadyExists() throws Exception {
        doThrow(new AcolhimentoException(ReasonCode.ANAMNESE_ALREADY_EXISTS, "corr", "Anamnese ja existe"))
                .when(acolhimentoAnamneseService).create(eq(PATIENT_ID), eq(NUCLEO_ID), any());

        mockMvc.perform(post("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(envelopeBody("11111111-1111-1111-1111-111111111111")))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-Correlation-ID", "corr"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.reasonCode").value("ANAMNESE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Anamnese ja existe"));
    }

    @Test
    void shouldReturn200OnUpdate() throws Exception {
        String correlationId = "22222222-2222-2222-2222-222222222222";
        when(acolhimentoAnamneseService.update(eq(PATIENT_ID), eq(NUCLEO_ID), any()))
                .thenReturn(new SuccessResponseDTO(
                        "ANAMNESE_UPDATED",
                        correlationId,
                        "Anamnese atualizada com sucesso",
                        new AnamneseDTO(LocalDate.of(2026, 3, 10), LocalTime.of(9, 0), "nova obs")));

        mockMvc.perform(put("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(envelopeBody(correlationId)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", correlationId))
                .andExpect(jsonPath("$.code").value("ANAMNESE_UPDATED"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.observacoes").value("nova obs"));

        verify(acolhimentoAnamneseService).update(eq(PATIENT_ID), eq(NUCLEO_ID), any());
    }

    @Test
    void shouldReturn404OnUpdateWhenAnamneseNotFound() throws Exception {
        doThrow(new AcolhimentoException(ReasonCode.ANAMNESE_NOT_FOUND, "corr", "Anamnese nao encontrada"))
                .when(acolhimentoAnamneseService).update(eq(PATIENT_ID), eq(NUCLEO_ID), any());

        mockMvc.perform(put("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(envelopeBody("22222222-2222-2222-2222-222222222222")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.reasonCode").value("ANAMNESE_NOT_FOUND"));
    }

    @Test
    void shouldReturn200OnDelete() throws Exception {
        String correlationId = "33333333-3333-3333-3333-333333333333";
        when(acolhimentoAnamneseService.delete(eq(PATIENT_ID), eq(NUCLEO_ID), any()))
                .thenReturn(new SuccessResponseDTO(
                        "ANAMNESE_DELETED",
                        correlationId,
                        "Anamnese removida com sucesso",
                        PATIENT_ID));

        mockMvc.perform(delete("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(deleteEnvelopeBody(correlationId)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", correlationId))
                .andExpect(jsonPath("$.code").value("ANAMNESE_DELETED"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.message").value("Anamnese removida com sucesso"))
                .andExpect(jsonPath("$.data").value(PATIENT_ID.toString()));

        verify(acolhimentoAnamneseService).delete(eq(PATIENT_ID), eq(NUCLEO_ID), any());
    }

    @Test
    void shouldReturn404OnDeleteWhenAnamneseNotFound() throws Exception {
        doThrow(new AcolhimentoException(ReasonCode.ANAMNESE_NOT_FOUND, "corr", "Anamnese nao encontrada"))
                .when(acolhimentoAnamneseService).delete(eq(PATIENT_ID), eq(NUCLEO_ID), any());

        mockMvc.perform(delete("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(deleteEnvelopeBody("33333333-3333-3333-3333-333333333333")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.reasonCode").value("ANAMNESE_NOT_FOUND"));
    }

    @Test
    void shouldReturn200OnRetrieve() throws Exception {
        when(acolhimentoAnamneseService.retrieve(PATIENT_ID, NUCLEO_ID))
                .thenReturn(new AnamneseDTO(LocalDate.of(2026, 3, 2), LocalTime.of(11, 35), "obs"));

        mockMvc.perform(get("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataAnamnese").value("2026-03-02"))
                .andExpect(jsonPath("$.horaAnamnese").value("11:35:00"))
                .andExpect(jsonPath("$.observacoes").value("obs"));

        verify(acolhimentoAnamneseService).retrieve(PATIENT_ID, NUCLEO_ID);
    }

    @Test
    void shouldReturn404OnRetrieveWhenAnamneseNotFound() throws Exception {
        when(acolhimentoAnamneseService.retrieve(PATIENT_ID, NUCLEO_ID))
                .thenThrow(new AcolhimentoException(ReasonCode.ANAMNESE_NOT_FOUND, "corr", "Anamnese nao encontrada"));

        mockMvc.perform(get("/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}", PATIENT_ID, NUCLEO_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.reasonCode").value("ANAMNESE_NOT_FOUND"));
    }

    private String envelopeBody(String correlationId) {
        return """
                {
                  "correlationId": "%s",
                  "producerService": "humanizar-admin-dashboard",
                  "occurredAt": "2026-03-02T14:00:00",
                  "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
                  "userAgent": "Mozilla/5.0",
                  "originIp": "187.11.22.33",
                  "payload": {
                    "dataAnamnese": "2026-03-02",
                    "horaAnamnese": "11:35:00",
                    "observacoes": "obs"
                  }
                }
                """.formatted(correlationId);
    }

    private String deleteEnvelopeBody(String correlationId) {
        return """
                {
                  "correlationId": "%s",
                  "producerService": "humanizar-admin-dashboard",
                  "occurredAt": "2026-03-02T14:00:00",
                  "actorId": "f8b9bf6f-39df-4ba8-b95e-5e5e526a725f",
                  "userAgent": "Mozilla/5.0",
                  "originIp": "187.11.22.33",
                  "payload": null
                }
                """.formatted(correlationId);
    }
}
