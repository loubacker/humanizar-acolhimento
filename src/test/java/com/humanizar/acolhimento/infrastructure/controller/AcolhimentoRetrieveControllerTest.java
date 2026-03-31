package com.humanizar.acolhimento.infrastructure.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoRetrieveService;
import com.humanizar.acolhimento.application.service.central.AcolhimentoCentralListService;
import com.humanizar.acolhimento.application.service.central.AcolhimentoCentralSnapshotService;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.controller.handler.AcolhimentoExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AcolhimentoRetrieveControllerTest {

        @Mock
        private AcolhimentoRetrieveService acolhimentoRetrieveService;

        @Mock
        private AcolhimentoCentralListService acolhimentoCentralListService;

        @Mock
        private AcolhimentoCentralSnapshotService acolhimentoCentralSnapshotService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                AcolhimentoRetrieveController controller = new AcolhimentoRetrieveController(
                                acolhimentoRetrieveService,
                                acolhimentoCentralListService,
                                acolhimentoCentralSnapshotService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new AcolhimentoExceptionHandler())
                                .build();
        }

        @Test
        void shouldReturn200WithInboundPayload() throws Exception {
                UUID patientId = UUID.fromString("f6bd95b5-d3f4-4f09-aa49-bf192d5ca7a9");
                UUID nucleoId = UUID.fromString("3fd17f7c-4541-4d31-bf9c-57618cd6f540");
                UUID responsavelId = UUID.fromString("8ce9ef36-5ea3-41cf-a2f8-89b4c2995236");

                InboundAcolhimentoDTO payload = new InboundAcolhimentoDTO(
                                patientId,
                                LocalDate.of(2026, 3, 11),
                                LocalTime.of(10, 30),
                                "Observacao de teste",
                                List.of(new NucleoPatientDTO(
                                                nucleoId,
                                                List.of(new NucleoResponsavelDTO(responsavelId, "COORDENADOR")))));

                when(acolhimentoRetrieveService.findByPatientId(patientId)).thenReturn(payload);

                mockMvc.perform(get("/api/v1/acolhimento/{patientId}", patientId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                                .andExpect(jsonPath("$.dataAnamnese").value("2026-03-11"))
                                .andExpect(jsonPath("$.horaAnamnese").value("10:30:00"))
                                .andExpect(jsonPath("$.observacoes").value("Observacao de teste"))
                                .andExpect(jsonPath("$.nucleoPatient[0].nucleoId").value(nucleoId.toString()))
                                .andExpect(jsonPath("$.nucleoPatient[0].nucleoPatientResponsavel[0].responsavelId")
                                                .value(responsavelId.toString()))
                                .andExpect(jsonPath("$.nucleoPatient[0].nucleoPatientResponsavel[0].role")
                                                .value("COORDENADOR"));

                verify(acolhimentoRetrieveService).findByPatientId(patientId);
        }

        @Test
        void shouldReturn404WhenPatientDoesNotExist() throws Exception {
                UUID patientId = UUID.fromString("9a1fd2b6-9da5-4a07-a15f-377acebd57dd");
                String correlationId = "corr-retrieve-404";

                when(acolhimentoRetrieveService.findByPatientId(patientId))
                                .thenThrow(new AcolhimentoException(
                                                ReasonCode.PATIENT_NOT_FOUND,
                                                correlationId,
                                                "Paciente nao encontrado"));

                mockMvc.perform(get("/api/v1/acolhimento/{patientId}", patientId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.reasonCode").value("PATIENT_NOT_FOUND"))
                                .andExpect(jsonPath("$.correlationId").value(correlationId))
                                .andExpect(jsonPath("$.path").value("/api/v1/acolhimento/" + patientId));

                verify(acolhimentoRetrieveService).findByPatientId(patientId);
        }
}
