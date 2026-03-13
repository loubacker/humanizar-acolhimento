package com.humanizar.acolhimento.infrastructure.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoCreateService;
import com.humanizar.acolhimento.infrastructure.controller.dto.AcolhimentoCreateResponseDTO;

@RestController
@RequestMapping("/api/v1/acolhimento")
public class AcolhimentoCreateController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoCreateController.class);

    private final AcolhimentoCreateService acolhimentoCreateService;

    public AcolhimentoCreateController(AcolhimentoCreateService acolhimentoCreateService) {
        this.acolhimentoCreateService = acolhimentoCreateService;
    }

    @PostMapping("/register")
    public ResponseEntity<AcolhimentoCreateResponseDTO> register(
            @RequestBody InboundEnvelopeDTO<InboundAcolhimentoDTO> envelope) {
        String correlationId = envelope != null && envelope.correlationId() != null
                ? envelope.correlationId().toString()
                : null;
        String patientId = envelope != null && envelope.payload() != null && envelope.payload().patientId() != null
                ? envelope.payload().patientId().toString()
                : null;

        log.info("Recebido POST register. correlationId={}, patientId={}, operacao=CREATE",
                correlationId, patientId);
        AcolhimentoCreateResponseDTO response = acolhimentoCreateService.register(envelope);
        log.info("POST register concluido com sucesso. correlationId={}, patientId={}",
                correlationId, patientId);
        return ResponseEntity.ok(response);
    }
}
