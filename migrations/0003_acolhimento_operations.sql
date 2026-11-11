BEGIN;

CREATE TABLE acolhimento_operations (
    operation_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    resource VARCHAR(512) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    owner UUID NOT NULL,
    state VARCHAR(16) NOT NULL,
    reason_code VARCHAR(100),
    response_status SMALLINT,
    response_body BYTEA,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_until TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_acolhimento_operations PRIMARY KEY (operation_id),
    CONSTRAINT ck_acolhimento_operations_action CHECK (action IN (
        'ACOLHIMENTO_CRIAR', 'ACOLHIMENTO_ATUALIZAR', 'ACOLHIMENTO_EXCLUIR',
        'ANAMNESE_REGISTRAR', 'ANAMNESE_ATUALIZAR', 'ANAMNESE_EXCLUIR',
        'AVALIACAO_VINCULAR', 'AVALIACAO_DESVINCULAR'
    )),
    CONSTRAINT ck_acolhimento_operations_state CHECK (state IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_acolhimento_operations_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_acolhimento_operations_resource CHECK (length(resource) > 0),
    CONSTRAINT ck_acolhimento_operations_response CHECK (
        (response_status IS NULL AND response_body IS NULL)
        OR (response_status BETWEEN 100 AND 599 AND response_body IS NOT NULL)
    )
);

CREATE INDEX idx_acolhimento_operations_actor_action
    ON acolhimento_operations (actor_id, action, operation_id);

CREATE INDEX idx_acolhimento_operations_pending_lease
    ON acolhimento_operations (lease_until, operation_id) WHERE state = 'PENDING';

COMMIT;
