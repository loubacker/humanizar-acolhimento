package com.humanizar.acolhimento.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveAcolhimentoUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientResponsavelUseCase;
import com.humanizar.acolhimento.application.usecase.retrieve.RetrieveNucleoPatientUseCase;
import com.humanizar.acolhimento.domain.model.acolhimento.Acolhimento;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;

@Service
public class AcolhimentoRetrieveService {

        private final RetrieveAcolhimentoUseCase retrieveAcolhimentoUseCase;
        private final RetrieveNucleoPatientUseCase retrieveNucleoPatientUseCase;
        private final RetrieveNucleoPatientResponsavelUseCase retrieveNucleoPatientResponsavelUseCase;

        public AcolhimentoRetrieveService(
                        RetrieveAcolhimentoUseCase findAcolhimentoByPatientIdRetrieveUseCase,
                        RetrieveNucleoPatientUseCase findNucleoPatientByPatientIdRetrieveUseCase,
                        RetrieveNucleoPatientResponsavelUseCase findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase) {
                this.retrieveAcolhimentoUseCase = findAcolhimentoByPatientIdRetrieveUseCase;
                this.retrieveNucleoPatientUseCase = findNucleoPatientByPatientIdRetrieveUseCase;
                this.retrieveNucleoPatientResponsavelUseCase = findNucleoPatientResponsavelByNucleoPatientIdRetrieveUseCase;
        }

        public InboundAcolhimentoDTO findByPatientId(UUID patientId) {
                String correlationId = UUID.randomUUID().toString();
                Acolhimento acolhimento = retrieveAcolhimentoUseCase.execute(patientId, correlationId);
                List<NucleoPatient> nucleoPatients = retrieveNucleoPatientUseCase.execute(patientId);

                List<NucleoPatientDTO> nucleoPatientDTOs = nucleoPatients.stream()
                                .map(nucleoPatient -> new NucleoPatientDTO(
                                                nucleoPatient.getNucleoId(),
                                                retrieveNucleoPatientResponsavelUseCase
                                                                .execute(nucleoPatient.getId()).stream()
                                                                .map(responsavel -> new NucleoResponsavelDTO(
                                                                                responsavel.getResponsavelId(),
                                                                                responsavel.getRole() != null
                                                                                                ? responsavel.getRole()
                                                                                                                .name()
                                                                                                : null))
                                                                .toList()))
                                .toList();

                return new InboundAcolhimentoDTO(
                                acolhimento.getPatientId(),
                                acolhimento.getDataAnamnese(),
                                acolhimento.getHoraAnamnese(),
                                acolhimento.getObservacoes(),
                                nucleoPatientDTOs);
        }
}
