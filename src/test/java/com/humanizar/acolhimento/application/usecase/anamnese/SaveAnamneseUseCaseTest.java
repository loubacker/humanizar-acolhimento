package com.humanizar.acolhimento.application.usecase.anamnese;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;

@ExtendWith(MockitoExtension.class)
class SaveAnamneseUseCaseTest {

    @Mock
    private AcolhimentoPort acolhimentoPort;

    @Mock
    private NucleoPatientPort nucleoPatientPort;

    @InjectMocks
    private SaveAnamneseUseCase useCase;

    @Test
    void shouldSaveAnamneseAndMarkNucleoPatientAsEntregue() {
        UUID nucleoPatientId = UUID.randomUUID();
        Acolhimento anamnese = Acolhimento.builder()
                .nucleoPatientId(nucleoPatientId)
                .build();
        Acolhimento saved = Acolhimento.builder()
                .id(UUID.randomUUID())
                .nucleoPatientId(nucleoPatientId)
                .build();

        when(acolhimentoPort.save(anamnese)).thenReturn(saved);

        Acolhimento result = useCase.execute(anamnese);

        assertEquals(saved, result);
        verify(acolhimentoPort).save(anamnese);
        verify(nucleoPatientPort).updateAnamneseStatus(nucleoPatientId, AnamneseStatus.ENTREGUE);
    }
}
