package com.humanizar.acolhimento.domain.model.enums;

public enum ReasonCode {

    // ============================================================
    //  400 — Bad Request
    // ============================================================

    // -- Requisicao generica
    VALIDATION_ERROR(400, "Falha de validacao do payload/evento.", false),
    REQUEST_BODY_INVALID(400, "Corpo da requisicao invalido ou ilegivel.", false),
    REQUEST_PARAMETER_INVALID(400, "Parametro da requisicao invalido.", false),
    REQUEST_PARAMETER_MISSING(400, "Parametro obrigatorio ausente na requisicao.", false),

    // -- Mensageria inbound
    INBOUND_REQUIRED_FIELD(400, "Campo obrigatorio ausente no inbound.", false),
    INBOUND_PARSE_ERROR(400, "Falha ao parsear mensagem inbound.", false),
    INBOUND_INVALID_ENUM(400, "Valor de enum invalido no inbound.", false),
    INBOUND_CONTEXT_INCONSISTENT(400, "Inconsistencia entre envelop.payload e payload.", false),
    INBOUND_PATIENT_MISMATCH(400, "patientId do payload diverge do patientId do path.", false),
    INBOUND_EMPTY_COLLECTION(400, "Colecao obrigatoria vazia no inbound.", false),
    INBOUND_DUPLICATE_ITEM(400, "Item duplicado em colecao inbound.", false),
    UNSUPPORTED_ROUTING_KEY(400, "Routing key nao suportada.", false),

    // ============================================================
    //  404 — Not Found
    // ============================================================

    PATIENT_NOT_FOUND(404, "Paciente nao encontrado em Acolhimento.", false),
    NUCLEO_PATIENT_NOT_FOUND(404, "NucleoPatient nao encontrado.", false),
    NUCLEO_NOT_REGISTERED(404, "Nucleo nao cadastrado para o paciente informado.", false),
    ANAMNESE_NOT_FOUND(404, "Anamnese nao encontrada para o nucleo informado.", false),

    // ============================================================
    //  409 — Conflict
    // ============================================================

    // -- Nucleo / abordagem
    HAS_ABORDAGEM(409, "Nao e permitido remover nucleo com abordagem vinculada.", false),
    ABORDAGEM_DUPLICATED(409, "Abordagem ja vinculada para este nucleoPatient.", false),

    // -- Acolhimento (duplicidade e saga)
    DUPLICATE_PATIENT(409, "Acolhimento ja existe para o patientId informado.", false),
    DELETE_IN_PROGRESS(409, "Ja existe operacao DELETE pendente para o patientId informado.", false),

    // -- Anamnese
    ANAMNESE_ALREADY_EXISTS(409, "Anamnese ja existe para o nucleo informado.", false),

    // -- Idempotencia e eventos
    DUPLICATE_EVENT(409, "Evento duplicado ja processado.", false),

    // ============================================================
    //  422 — Unprocessable Entity
    // ============================================================

    RESPONSAVEL_REQUIRED(422, "NucleoPatient exige ao menos um responsavel.", false),
    UNSUPPORTED_EVENT_VERSION(422, "Versao de evento nao suportada.", false),

    // ============================================================
    //  500 — Internal Server Error
    // ============================================================

    UNEXPECTED_ERROR(500, "Erro interno inesperado.", false),

    // ============================================================
    //  502 — Bad Gateway (integracoes externas)
    // ============================================================

    INTEGRATION_FAILURE(502, "Falha de integracao com dependencia externa.", true),

    // ============================================================
    //  503 — Service Unavailable
    // ============================================================

    PERSISTENCE_FAILURE(503, "Falha de persistencia no banco de dados.", true);

    private final int httpStatus;
    private final String message;
    private final boolean retryable;

    ReasonCode(int httpStatus, String message, boolean retryable) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.retryable = retryable;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }

    public int getStatusCode() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
