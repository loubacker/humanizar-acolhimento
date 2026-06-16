package com.humanizar.acolhimento.application.service;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.application.http.dto.anamnese.AcolhimentoListResponseDTO;
import com.humanizar.acolhimento.application.usecase.snapshot.BuildPatientSnapshotUseCase;

@ExtendWith(MockitoExtension.class)
class AcolhimentoRetrieveServiceTest {

        @Mock
        private BuildPatientSnapshotUseCase buildPatientSnapshotUseCase;

        private AcolhimentoRetrieveService service;

        @BeforeEach
        void setUp() {
                service = new AcolhimentoRetrieveService(buildPatientSnapshotUseCase);
        }

        @Test
        void shouldDelegateToSnapshotBuilder() {
                UUID patientId = UUID.randomUUID();
                AcolhimentoListResponseDTO expected = new AcolhimentoListResponseDTO(patientId, List.of());

                when(buildPatientSnapshotUseCase.execute(patientId)).thenReturn(expected);

                AcolhimentoListResponseDTO response = service.findByPatientId(patientId);

                assertSame(expected, response);
                assertEquals(patientId, response.patientId());
                verify(buildPatientSnapshotUseCase).execute(patientId);
        }
}
