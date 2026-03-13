package com.humanizar.acolhimento.domain.model.enums;

public enum ReasonCode {

    HAS_ABORDAGEM(409, "Nao e permitido remover nucleo com abordagem vinculada.", false),
    RESPONSAVEL_REQUIRED(422, "NucleoPatient exige ao menos um responsavel.", false),
    PATIENT_NOT_FOUND(404, "Paciente não encontrado em Acolhimento.", false),
    NUCLEO_PATIENT_NOT_FOUND(404, "NucleoPatient nao encontrado.", false),
    ABORDAGEM_DUPLICATED(409, "Abordagem ja vinculada para este nucleoPatient.", false),
    VALIDATION_ERROR(400, "Falha de validacao do payload/evento.", false),
    INBOUND_REQUIRED_FIELD(400, "Campo obrigatorio ausente no inbound.", false),
    INBOUND_INVALID_ENUM(400, "Valor de enum invalido no inbound.", false),
    INBOUND_CONTEXT_INCONSISTENT(400, "Inconsistencia entre envelop.payload e payload.", false),
    INBOUND_PATIENT_MISMATCH(400, "patientId do payload diverge do patientId do path.", false),
    INBOUND_EMPTY_COLLECTION(400, "Colecao obrigatoria vazia no inbound.", false),
    INBOUND_DUPLICATE_ITEM(400, "Item duplicado em colecao inbound.", false),
    DUPLICATE_PATIENT(409, "Acolhimento ja existe para o patientId informado.", false),
    UNSUPPORTED_EVENT_VERSION(422, "Versao de evento nao suportada.", false),
    UNSUPPORTED_ROUTING_KEY(400, "Routing key nao suportada.", false),
    DUPLICATE_EVENT(409, "Evento duplicado ja processado.", false),
    INTEGRATION_FAILURE(502, "Falha de integracao com dependencia externa.", true),
    PERSISTENCE_FAILURE(503, "Falha de persistencia no banco de dados.", true);

    private final int statusCode;
    private final String message;
    private final boolean retryable;

    ReasonCode(int statusCode, String message, boolean retryable) {
        this.statusCode = statusCode;
        this.message = message;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
