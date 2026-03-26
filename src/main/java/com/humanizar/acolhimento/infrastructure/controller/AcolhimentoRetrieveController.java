package com.humanizar.acolhimento.infrastructure.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoRetrieveService;
import com.humanizar.acolhimento.infrastructure.config.ResilientMethodsConfig.Retry;

@RestController
@RequestMapping("/api/v1/acolhimento")
public class AcolhimentoRetrieveController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoRetrieveController.class);

    private final AcolhimentoRetrieveService acolhimentoRetrieveService;

    public AcolhimentoRetrieveController(AcolhimentoRetrieveService acolhimentoRetrieveService) {
        this.acolhimentoRetrieveService = acolhimentoRetrieveService;
    }

    @Retry
    @GetMapping("/{patientId}")
    public ResponseEntity<InboundAcolhimentoDTO> retrieve(@PathVariable UUID patientId) {
        log.info("Recebido GET /api/v1/acolhimento/{}. operacao=RETRIEVE", patientId);
        InboundAcolhimentoDTO payload = acolhimentoRetrieveService.findByPatientId(patientId);
        log.info("GET /api/v1/acolhimento/{} Concluido com Sucesso", patientId);
        return ResponseEntity.ok(payload);
    }
}
