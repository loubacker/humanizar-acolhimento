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

import com.humanizar.acolhimento.application.http.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.http.mapper.HttpEnvelopeMapper;
import com.humanizar.acolhimento.application.service.AcolhimentoDeleteService;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@RestController
@RequestMapping("/api/v1/acolhimento")
public class AcolhimentoDeleteController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoDeleteController.class);

    private final AcolhimentoDeleteService acolhimentoDeleteService;
    private final HttpEnvelopeMapper inboundEnvelopeMapper;

    public AcolhimentoDeleteController(
            AcolhimentoDeleteService acolhimentoDeleteService,
            HttpEnvelopeMapper inboundEnvelopeMapper) {
        this.acolhimentoDeleteService = acolhimentoDeleteService;
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
    }

    @DeleteMapping("/delete/{patientId}")
    public ResponseEntity<SuccessResponseDTO> delete(
            @PathVariable UUID patientId,
            @RequestBody(required = false) HttpEnvelopeDTO<AcolhimentoDeleteDTO> envelope) {
        String correlationId = inboundEnvelopeMapper.correlationIdAsString(envelope);
        String payloadPatientId = inboundEnvelopeMapper.payloadFieldAsString(envelope, AcolhimentoDeleteDTO::patientId);

        log.info("Recebido DELETE /api/v1/acolhimento/delete/{}. correlationId={}, payloadPatientId={}, operacao=DELETE",
                patientId, correlationId, payloadPatientId);
        SuccessResponseDTO response = acolhimentoDeleteService.deleteByPatientId(patientId, envelope);
        log.info("DELETE /api/v1/acolhimento/delete/{} Concluido com Sucesso. correlationId={}",
                patientId, correlationId);

        return ResponseEntity.ok()
                .header("X-Correlation-ID", response.correlationId())
                .body(response);
    }
}
