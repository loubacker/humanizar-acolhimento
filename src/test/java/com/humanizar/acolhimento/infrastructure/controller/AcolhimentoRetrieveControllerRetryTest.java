package com.humanizar.acolhimento.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.humanizar.acolhimento.application.http.dto.anamnese.AcolhimentoListResponseDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoRetrieveService;
import com.humanizar.acolhimento.application.service.central.AcolhimentoCentralListService;
import com.humanizar.acolhimento.application.service.central.AcolhimentoCentralSnapshotService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AcolhimentoRetrieveControllerRetryTest.TestConfig.class)
class AcolhimentoRetrieveControllerRetryTest {

    @Configuration(proxyBeanMethods = false)
    @EnableResilientMethods
    static class TestConfig {

        @Bean
        AcolhimentoRetrieveService acolhimentoRetrieveService() {
            return mock(AcolhimentoRetrieveService.class);
        }

        @Bean
        AcolhimentoCentralListService acolhimentoCentralListService() {
            return mock(AcolhimentoCentralListService.class);
        }

        @Bean
        AcolhimentoCentralSnapshotService acolhimentoCentralSnapshotService() {
            return mock(AcolhimentoCentralSnapshotService.class);
        }

        @Bean
        AcolhimentoRetrieveController acolhimentoRetrieveController(
                AcolhimentoRetrieveService service,
                AcolhimentoCentralListService centralListService,
                AcolhimentoCentralSnapshotService centralSnapshotService) {
            return new AcolhimentoRetrieveController(service, centralListService, centralSnapshotService);
        }
    }

    @Autowired
    private AcolhimentoRetrieveController controller;

    @Autowired
    private AcolhimentoRetrieveService service;

    @Test
    void shouldReturn200WithoutRetryWhenSuccessOnFirstAttempt() {
        UUID patientId = UUID.fromString("f6bd95b5-d3f4-4f09-aa49-bf192d5ca7a9");
        AcolhimentoListResponseDTO payload = samplePayload(patientId);

        when(service.findByPatientId(patientId)).thenReturn(payload);

        ResponseEntity<AcolhimentoListResponseDTO> response = controller.retrieve(patientId);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(patientId, response.getBody().patientId());
        verify(service, times(1)).findByPatientId(patientId);
    }

    @Test
    void shouldRetryTransientFailureAndSucceedOnThirdAttempt() {
        UUID patientId = UUID.fromString("c7e3d934-c84a-4a3f-8a66-f0a9f7d22111");
        AcolhimentoListResponseDTO payload = samplePayload(patientId);

        when(service.findByPatientId(patientId))
                .thenThrow(new TransientDataAccessResourceException("temporary failure 1"))
                .thenThrow(new TransientDataAccessResourceException("temporary failure 2"))
                .thenReturn(payload);

        ResponseEntity<AcolhimentoListResponseDTO> response = controller.retrieve(patientId);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(patientId, response.getBody().patientId());
        verify(service, times(3)).findByPatientId(patientId);
    }

    private AcolhimentoListResponseDTO samplePayload(UUID patientId) {
        return new AcolhimentoListResponseDTO(patientId, List.of());
    }
}
