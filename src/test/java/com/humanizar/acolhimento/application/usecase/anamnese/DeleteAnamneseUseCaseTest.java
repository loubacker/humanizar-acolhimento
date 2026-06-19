package com.humanizar.acolhimento.application.usecase.anamnese;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.humanizar.acolhimento.domain.model.enums.AnamneseStatus;
import com.humanizar.acolhimento.domain.port.acolhimento.AcolhimentoPort;
import com.humanizar.acolhimento.domain.port.nucleo.NucleoPatientPort;

@ExtendWith(MockitoExtension.class)
class DeleteAnamneseUseCaseTest {

    @Mock
    private AcolhimentoPort acolhimentoPort;

    @Mock
    private NucleoPatientPort nucleoPatientPort;

    @InjectMocks
    private DeleteAnamneseUseCase useCase;

    @Test
    void shouldDeleteAnamneseAndMarkNucleoPatientAsPendente() {
        UUID nucleoPatientId = UUID.randomUUID();

        useCase.execute(nucleoPatientId);

        verify(acolhimentoPort).deleteByNucleoPatientId(nucleoPatientId);
        verify(nucleoPatientPort).updateAnamneseStatus(nucleoPatientId, AnamneseStatus.PENDENTE);
    }
}
