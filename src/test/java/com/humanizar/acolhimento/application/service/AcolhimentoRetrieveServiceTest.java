package com.humanizar.acolhimento.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.enums.ResponsavelRole;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;

@ExtendWith(MockitoExtension.class)
class AcolhimentoRetrieveServiceTest {

        @Mock
        private RetrieveAcolhimentoUseCase findAcolhimentoByPatientIdRetrieveUseCase;

        @Mock
        private RetrieveNucleoPatientUseCase findNucleoPatientByPatientIdRetrieveUseCase;

        @Mock
        private RetrieveNucleoPatientResponsavelUseCase findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase;

        private AcolhimentoRetrieveService service;

        @BeforeEach
        @SuppressWarnings("unused")
        void setUp() {
                service = new AcolhimentoRetrieveService(
                                findAcolhimentoByPatientIdRetrieveUseCase,
                                findNucleoPatientByPatientIdRetrieveUseCase,
                                findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase);
        }

        @Test
        void shouldReturnInboundPayloadWithNestedNucleosAndResponsaveis() {
                UUID patientId = UUID.randomUUID();
                UUID nucleoPatientId = UUID.randomUUID();
                UUID nucleoId = UUID.randomUUID();
                UUID responsavelId = UUID.randomUUID();

                Acolhimento acolhimento = Acolhimento.builder()
                                .id(UUID.randomUUID())
                                .patientId(patientId)
                                .dataAnamnese(LocalDate.of(2026, 3, 11))
                                .horaAnamnese(LocalTime.of(10, 30))
                                .observacoes("observacao de teste")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                NucleoPatient nucleoPatient = NucleoPatient.builder()
                                .id(nucleoPatientId)
                                .patientId(patientId)
                                .nucleoId(nucleoId)
                                .nucleoPatientResponsavel(List.of())
                                .build();

                NucleoPatientResponsavel responsavel = NucleoPatientResponsavel.builder()
                                .id(UUID.randomUUID())
                                .nucleoPatientId(nucleoPatientId)
                                .responsavelId(responsavelId)
                                .role(ResponsavelRole.ADMINISTRADOR)
                                .build();

                when(findAcolhimentoByPatientIdRetrieveUseCase.execute(eq(patientId), anyString()))
                                .thenReturn(acolhimento);
                when(findNucleoPatientByPatientIdRetrieveUseCase.execute(patientId))
                                .thenReturn(List.of(nucleoPatient));
                when(findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase.execute(nucleoPatientId))
                                .thenReturn(List.of(responsavel));

                InboundAcolhimentoDTO response = service.findByPatientId(patientId);

                assertEquals(patientId, response.patientId());
                assertEquals(LocalDate.of(2026, 3, 11), response.dataAnamnese());
                assertEquals(LocalTime.of(10, 30), response.horaAnamnese());
                assertEquals("observacao de teste", response.observacoes());
                assertEquals(1, response.nucleoPatient().size());
                assertEquals(nucleoId, response.nucleoPatient().get(0).nucleoId());
                assertEquals(1, response.nucleoPatient().get(0).nucleoPatientResponsavel().size());
                assertEquals(responsavelId,
                                response.nucleoPatient().get(0).nucleoPatientResponsavel().get(0).responsavelId());
                assertEquals("ADMINISTRADOR", response.nucleoPatient().get(0).nucleoPatientResponsavel().get(0).role());
        }

        @Test
        void shouldThrowPatientNotFoundWhenAcolhimentoDoesNotExist() {
                UUID patientId = UUID.randomUUID();
                AcolhimentoException notFound = new AcolhimentoException(
                                ReasonCode.PATIENT_NOT_FOUND,
                                "corr-retrieve",
                                "Paciente nao encontrado");

                when(findAcolhimentoByPatientIdRetrieveUseCase.execute(eq(patientId), anyString()))
                                .thenThrow(notFound);

                AcolhimentoException thrown = assertThrows(AcolhimentoException.class,
                                () -> service.findByPatientId(patientId));

                assertEquals(ReasonCode.PATIENT_NOT_FOUND, thrown.getReasonCode());
                verify(findNucleoPatientByPatientIdRetrieveUseCase, never()).execute(patientId);
        }
}
