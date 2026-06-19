package com.humanizar.acolhimento.application.usecase.snapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.http.dto.anamnese.AcolhimentoListResponseDTO;
import com.humanizar.acolhimento.application.http.mapper.AnamneseMapper;
import com.humanizar.acolhimento.application.http.mapper.InboundFieldValidator;
import com.humanizar.acolhimento.application.serializer.JsonPayloadSerializer;
import com.humanizar.acolhimento.application.usecase.anamnese.RetrieveAnamneseUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.model.enums.ResponsavelRole;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;

@ExtendWith(MockitoExtension.class)
class BuildPatientSnapshotUseCaseTest {

        @Mock
        private RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase;

        @Mock
        private RetrieveNucleoPatientResponsavelUseCase retrieveNucleoPatientResponsavelUseCase;

        @Mock
        private RetrieveAnamneseUseCase retrieveAnamneseUseCase;

        private BuildPatientSnapshotUseCase useCase;

        @BeforeEach
        void setUp() {
                useCase = new BuildPatientSnapshotUseCase(
                                retrieveNucleoPatientUseCase,
                                retrieveNucleoPatientResponsavelUseCase,
                                retrieveAnamneseUseCase,
                                new AnamneseMapper(new InboundFieldValidator()),
                                new JsonPayloadSerializer(new ObjectMapper()));
        }

        @Test
        void shouldReturnNucleosWithResponsaveisAndEmbeddedAnamnese() {
                UUID patientId = UUID.randomUUID();
                UUID nucleoPatientId = UUID.randomUUID();
                UUID nucleoId = UUID.randomUUID();
                UUID responsavelId = UUID.randomUUID();

                NucleoPatient nucleoPatient = NucleoPatient.builder()
                                .id(nucleoPatientId)
                                .patientId(patientId)
                                .nucleoId(nucleoId)
                                .anamneseStatus(AnamneseStatus.ENTREGUE)
                                .nucleoPatientResponsavel(List.of())
                                .build();

                NucleoPatientResponsavel responsavel = NucleoPatientResponsavel.builder()
                                .id(UUID.randomUUID())
                                .nucleoPatientId(nucleoPatientId)
                                .responsavelId(responsavelId)
                                .role(ResponsavelRole.ADMINISTRADOR)
                                .build();

                Acolhimento anamnese = Acolhimento.builder()
                                .id(UUID.randomUUID())
                                .nucleoPatientId(nucleoPatientId)
                                .dataAnamnese(LocalDate.of(2026, 3, 11))
                                .horaAnamnese(LocalTime.of(10, 30))
                                .observacoes("observacao de teste")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                when(retrieveNucleoPatientUseCase.execute(patientId))
                                .thenReturn(List.of(nucleoPatient));
                when(retrieveNucleoPatientResponsavelUseCase.execute(nucleoPatientId))
                                .thenReturn(List.of(responsavel));
                when(retrieveAnamneseUseCase.executeBatch(List.of(nucleoPatientId)))
                                .thenReturn(List.of(anamnese));

                AcolhimentoListResponseDTO response = useCase.execute(patientId);

                assertEquals(patientId, response.patientId());
                assertEquals(1, response.nucleos().size());
                assertEquals(nucleoId, response.nucleos().get(0).nucleoId());
                assertEquals(1, response.nucleos().get(0).responsaveis().size());
                assertEquals(responsavelId, response.nucleos().get(0).responsaveis().get(0).responsavelId());
                assertEquals("ADMINISTRADOR", response.nucleos().get(0).responsaveis().get(0).role());
                assertEquals(LocalDate.of(2026, 3, 11), response.nucleos().get(0).anamnese().dataAnamnese());
                assertEquals(LocalTime.of(10, 30), response.nucleos().get(0).anamnese().horaAnamnese());
                assertEquals("observacao de teste", response.nucleos().get(0).anamnese().observacoes());
                assertEquals("ENTREGUE", response.nucleos().get(0).anamneseStatus());
        }

        @Test
        void shouldReturnNucleoWithNullAnamneseWhenNoneRegistered() {
                UUID patientId = UUID.randomUUID();
                UUID nucleoPatientId = UUID.randomUUID();
                UUID nucleoId = UUID.randomUUID();

                NucleoPatient nucleoPatient = NucleoPatient.builder()
                                .id(nucleoPatientId)
                                .patientId(patientId)
                                .nucleoId(nucleoId)
                                .nucleoPatientResponsavel(List.of())
                                .build();

                when(retrieveNucleoPatientUseCase.execute(patientId))
                                .thenReturn(List.of(nucleoPatient));
                when(retrieveNucleoPatientResponsavelUseCase.execute(nucleoPatientId))
                                .thenReturn(List.of());
                when(retrieveAnamneseUseCase.executeBatch(List.of(nucleoPatientId)))
                                .thenReturn(List.of());

                AcolhimentoListResponseDTO response = useCase.execute(patientId);

                assertEquals(patientId, response.patientId());
                assertEquals(1, response.nucleos().size());
                assertEquals(nucleoId, response.nucleos().get(0).nucleoId());
                assertNull(response.nucleos().get(0).anamnese());
        }

        @Test
        void shouldSerializeCompleteSnapshotIncludingPatientId() {
                UUID patientId = UUID.randomUUID();
                UUID nucleoPatientId = UUID.randomUUID();
                UUID nucleoId = UUID.randomUUID();

                NucleoPatient nucleoPatient = NucleoPatient.builder()
                                .id(nucleoPatientId)
                                .patientId(patientId)
                                .nucleoId(nucleoId)
                                .nucleoPatientResponsavel(List.of())
                                .build();

                when(retrieveNucleoPatientUseCase.execute(patientId))
                                .thenReturn(List.of(nucleoPatient));
                when(retrieveNucleoPatientResponsavelUseCase.execute(nucleoPatientId))
                                .thenReturn(List.of());
                when(retrieveAnamneseUseCase.executeBatch(List.of(nucleoPatientId)))
                                .thenReturn(List.of());

                String json = useCase.serialize(patientId, patientId.toString());

                assertTrue(json.contains(patientId.toString()));
                assertTrue(json.contains(nucleoId.toString()));
                assertTrue(json.contains("\"anamnese\":null"));
        }
}
