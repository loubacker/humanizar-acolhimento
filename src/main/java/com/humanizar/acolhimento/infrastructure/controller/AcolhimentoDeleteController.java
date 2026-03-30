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
import com.humanizar.acolhimento.application.inbound.mapper.InboundEnvelopeMapper;
import com.humanizar.acolhimento.application.service.AcolhimentoDeleteService;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoDeleteResponseDTO;

@RestController
@RequestMapping("/api/v1/acolhimento")
public class AcolhimentoDeleteController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoDeleteController.class);

    private final AcolhimentoDeleteService acolhimentoDeleteService;
    private final InboundEnvelopeMapper inboundEnvelopeMapper;

    public AcolhimentoDeleteController(
            AcolhimentoDeleteService acolhimentoDeleteService,
            InboundEnvelopeMapper inboundEnvelopeMapper) {
        this.acolhimentoDeleteService = acolhimentoDeleteService;
        this.inboundEnvelopeMapper = inboundEnvelopeMapper;
    }

    @DeleteMapping("/delete/{patientId}")
    public ResponseEntity<AcolhimentoDeleteResponseDTO> delete(
            @PathVariable UUID patientId,
            @RequestBody(required = false) InboundEnvelopeDTO<AcolhimentoDeleteDTO> envelope) {
        String correlationId = inboundEnvelopeMapper.correlationIdAsString(envelope);
        String payloadPatientId = inboundEnvelopeMapper.payloadFieldAsString(envelope, AcolhimentoDeleteDTO::patientId);

        log.info("Recebido DELETE /api/v1/acolhimento/delete/{}. correlationId={}, payloadPatientId={}, operacao=DELETE",
                patientId, correlationId, payloadPatientId);
        acolhimentoDeleteService.deleteByPatientId(patientId, envelope);
        log.info("DELETE /api/v1/acolhimento/delete/{} Concluido com Sucesso. correlationId={}",
                patientId, correlationId);

        return ResponseEntity.ok(new AcolhimentoDeleteResponseDTO(
                "SUCCESS",
                "DELETE",
                patientId));
    }
}
