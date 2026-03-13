package com.humanizar.acolhimento.infrastructure.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoDeleteService;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoDeleteResponseDTO;

@RestController
@RequestMapping("/api/v1/acolhimento")
public class AcolhimentoDeleteController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoDeleteController.class);

    private final AcolhimentoDeleteService acolhimentoDeleteService;

    public AcolhimentoDeleteController(AcolhimentoDeleteService acolhimentoDeleteService) {
        this.acolhimentoDeleteService = acolhimentoDeleteService;
    }

    @DeleteMapping("/delete/{patientId}")
    public ResponseEntity<AcolhimentoDeleteResponseDTO> delete(
            @PathVariable UUID patientId,
            @RequestBody(required = false) InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelop) {
        String correlationId = envelop != null && envelop.correlationId() != null
                ? envelop.correlationId().toString()
                : null;
        String payloadPatientId = envelop != null && envelop.payload() != null && envelop.payload().patientId() != null
                ? envelop.payload().patientId().toString()
                : null;

        log.info("Recebido DELETE /api/v1/acolhimento/delete/{}. correlationId={}, payloadPatientId={}, operacao=DELETE",
                patientId, correlationId, payloadPatientId);
        acolhimentoDeleteService.deleteByPatientId(patientId, envelop);
        log.info("DELETE /api/v1/acolhimento/delete/{} Concluido com Sucesso. correlationId={}",
                patientId, correlationId);

        return ResponseEntity.ok(new AcolhimentoDeleteResponseDTO(
                "SUCCESS",
                "DELETE",
                patientId));
    }
}
