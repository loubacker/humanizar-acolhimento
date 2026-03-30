package com.humanizar.acolhimento.infrastructure.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.outbound.dto.central.PendingCentralPageDTO;
import com.humanizar.acolhimento.application.outbound.dto.central.PendingCentralSnapshotDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoRetrieveService;
import com.humanizar.acolhimento.application.service.central.AcolhimentoCentralListService;
import com.humanizar.acolhimento.application.service.central.AcolhimentoCentralSnapshotService;
import com.humanizar.acolhimento.infrastructure.config.ResilientMethodsConfig.Retry;

@RestController
@RequestMapping("/api/v1/acolhimento")
public class AcolhimentoRetrieveController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoRetrieveController.class);

    private final AcolhimentoRetrieveService acolhimentoRetrieveService;
    private final AcolhimentoCentralListService acolhimentoCentralListService;
    private final AcolhimentoCentralSnapshotService acolhimentoCentralSnapshotService;

    public AcolhimentoRetrieveController(
            AcolhimentoRetrieveService acolhimentoRetrieveService,
            AcolhimentoCentralListService acolhimentoCentralListService,
            AcolhimentoCentralSnapshotService acolhimentoCentralSnapshotService) {
        this.acolhimentoRetrieveService = acolhimentoRetrieveService;
        this.acolhimentoCentralListService = acolhimentoCentralListService;
        this.acolhimentoCentralSnapshotService = acolhimentoCentralSnapshotService;
    }

    @Retry
    @GetMapping("/{patientId}")
    public ResponseEntity<InboundAcolhimentoDTO> retrieve(@PathVariable UUID patientId) {
        log.info("Recebido GET /api/v1/acolhimento/{}. operacao=RETRIEVE", patientId);
        InboundAcolhimentoDTO payload = acolhimentoRetrieveService.findByPatientId(patientId);
        log.info("GET /api/v1/acolhimento/{} Concluido com Sucesso", patientId);
        return ResponseEntity.ok(payload);
    }

    @Retry
    @GetMapping("/central/{patientId}")
    public ResponseEntity<PendingCentralPageDTO> listCentral(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Recebido GET /api/v1/acolhimento/central/{}. page={}, size={}", patientId, page, size);
        PendingCentralPageDTO response = acolhimentoCentralListService.execute(patientId, page, size);
        log.info("GET /api/v1/acolhimento/central/{} Concluido. totalElements={}", patientId, response.totalElements());
        return ResponseEntity.ok(response);
    }

    @Retry
    @GetMapping("/central/snapshot/{eventId}")
    public ResponseEntity<PendingCentralSnapshotDTO> snapshot(@PathVariable UUID eventId) {
        log.info("Recebido GET /api/v1/acolhimento/central/snapshot/{}. operacao=SNAPSHOT", eventId);
        return acolhimentoCentralSnapshotService.execute(eventId)
                .map(dto -> {
                    log.info("GET /api/v1/acolhimento/central/snapshot/{} Concluido com Sucesso", eventId);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    log.warn("GET /api/v1/acolhimento/central/snapshot/{} Nao encontrado", eventId);
                    return ResponseEntity.notFound().build();
                });
    }
}
