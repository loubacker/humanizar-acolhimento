package com.humanizar.acolhimento.infrastructure.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.humanizar.acolhimento.application.http.dto.anamnese.AnamneseDTO;
import com.humanizar.acolhimento.application.http.dto.envelop.HttpEnvelopeDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoAnamneseService;
import com.humanizar.acolhimento.infrastructure.config.ResilientMethodsConfig.Retry;
import com.humanizar.acolhimento.infrastructure.controller.dto.SuccessResponseDTO;

@RestController
@RequestMapping("/api/v1/acolhimento/anamnese")
public class AcolhimentoAnamneseController {

    private static final Logger log = LoggerFactory.getLogger(AcolhimentoAnamneseController.class);

    private final AcolhimentoAnamneseService acolhimentoAnamneseService;

    public AcolhimentoAnamneseController(AcolhimentoAnamneseService acolhimentoAnamneseService) {
        this.acolhimentoAnamneseService = acolhimentoAnamneseService;
    }

    @PostMapping("/{patientId}/{nucleoId}")
    public ResponseEntity<SuccessResponseDTO> create(
            @PathVariable UUID patientId,
            @PathVariable UUID nucleoId,
            @RequestBody HttpEnvelopeDTO<AnamneseDTO> envelope) {
        log.info("Recebido POST anamnese. patientId={}, nucleoId={}, operacao=CREATE", patientId, nucleoId);
        SuccessResponseDTO response = acolhimentoAnamneseService.create(patientId, nucleoId, envelope);
        log.info("POST anamnese concluido com sucesso. patientId={}, nucleoId={}", patientId, nucleoId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Correlation-ID", response.correlationId())
                .body(response);
    }

    @PutMapping("/{patientId}/{nucleoId}")
    public ResponseEntity<SuccessResponseDTO> update(
            @PathVariable UUID patientId,
            @PathVariable UUID nucleoId,
            @RequestBody HttpEnvelopeDTO<AnamneseDTO> envelope) {
        log.info("Recebido PUT anamnese. patientId={}, nucleoId={}, operacao=UPDATE", patientId, nucleoId);
        SuccessResponseDTO response = acolhimentoAnamneseService.update(patientId, nucleoId, envelope);
        log.info("PUT anamnese concluido com sucesso. patientId={}, nucleoId={}", patientId, nucleoId);
        return ResponseEntity.ok()
                .header("X-Correlation-ID", response.correlationId())
                .body(response);
    }

    @DeleteMapping("/{patientId}/{nucleoId}")
    public ResponseEntity<SuccessResponseDTO> delete(
            @PathVariable UUID patientId,
            @PathVariable UUID nucleoId,
            @RequestBody HttpEnvelopeDTO<AnamneseDTO> envelope) {
        log.info("Recebido DELETE anamnese. patientId={}, nucleoId={}, operacao=DELETE", patientId, nucleoId);
        SuccessResponseDTO response = acolhimentoAnamneseService.delete(patientId, nucleoId, envelope);
        log.info("DELETE anamnese concluido com sucesso. patientId={}, nucleoId={}", patientId, nucleoId);
        return ResponseEntity.ok()
                .header("X-Correlation-ID", response.correlationId())
                .body(response);
    }

    @Retry
    @GetMapping("/{patientId}/{nucleoId}")
    public ResponseEntity<AnamneseDTO> retrieve(
            @PathVariable UUID patientId,
            @PathVariable UUID nucleoId) {
        log.info("Recebido GET anamnese. patientId={}, nucleoId={}, operacao=RETRIEVE", patientId, nucleoId);
        AnamneseDTO response = acolhimentoAnamneseService.retrieve(patientId, nucleoId);
        log.info("GET anamnese concluido com sucesso. patientId={}, nucleoId={}", patientId, nucleoId);
        return ResponseEntity.ok(response);
    }
}
