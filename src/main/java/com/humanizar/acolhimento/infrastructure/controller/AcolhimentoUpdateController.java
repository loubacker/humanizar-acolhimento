package com.humanizar.acolhimento.infrastructure.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoUpdateService;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoUpdateResponseDTO;

@RestController
@RequestMapping("/api/v1/acolhimento")
public class AcolhimentoUpdateController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoUpdateController.class);

    private final AcolhimentoUpdateService acolhimentoUpdateService;

    public AcolhimentoUpdateController(AcolhimentoUpdateService acolhimentoUpdateService) {
        this.acolhimentoUpdateService = acolhimentoUpdateService;
    }

    @PutMapping("/update/{patientId}")
    public ResponseEntity<AcolhimentoUpdateResponseDTO> update(
            @PathVariable UUID patientId,
            @RequestBody InboundEnvelopeDTO<InboundAcolhimentoDTO> envelope) {
        String correlationId = envelope != null && envelope.correlationId() != null
                ? envelope.correlationId().toString()
                : null;
        String payloadPatientId = envelope != null && envelope.payload() != null && envelope.payload().patientId() != null
                ? envelope.payload().patientId().toString()
                : null;

        log.info("Recebido PUT /api/v1/acolhimento/update/{}. correlationId={}, payloadPatientId={}, operacao=UPDATE",
                patientId, correlationId, payloadPatientId);
        AcolhimentoUpdateResponseDTO response = acolhimentoUpdateService.updateByPatientId(patientId, envelope);
        log.info("PUT /api/v1/acolhimento/update/{} concluido com sucesso. correlationId={}",
                patientId, correlationId);
        return ResponseEntity.ok(response);
    }
}

