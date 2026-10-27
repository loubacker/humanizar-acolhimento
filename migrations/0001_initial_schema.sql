BEGIN;

CREATE TABLE IF NOT EXISTS nucleo_patient (
    id UUID NOT NULL,
    patient_id UUID NOT NULL,
    nucleo_id UUID NOT NULL,
    anamnese JSONB,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT pk_nucleo_patient PRIMARY KEY (id),
    CONSTRAINT uk_nucleo_patient_patient_nucleo UNIQUE (patient_id, nucleo_id)
);

CREATE INDEX IF NOT EXISTS idx_nucleo_patient_patient_id
    ON nucleo_patient (patient_id);

CREATE INDEX IF NOT EXISTS idx_nucleo_patient_nucleo_id
    ON nucleo_patient (nucleo_id);

CREATE TABLE IF NOT EXISTS nucleo_patient_staff (
    id UUID NOT NULL,
    nucleo_patient_id UUID NOT NULL,
    staff_user_id UUID NOT NULL,
    role VARCHAR(100) NOT NULL,
    CONSTRAINT pk_nucleo_patient_staff PRIMARY KEY (id),
    CONSTRAINT uk_nucleo_patient_staff_user UNIQUE (nucleo_patient_id, staff_user_id),
    CONSTRAINT fk_nucleo_patient_staff_nucleo_patient
        FOREIGN KEY (nucleo_patient_id)
        REFERENCES nucleo_patient (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nucleo_patient_staff_np_id
    ON nucleo_patient_staff (nucleo_patient_id);

CREATE INDEX IF NOT EXISTS idx_nucleo_patient_staff_staff_user_id
    ON nucleo_patient_staff (staff_user_id);

CREATE TABLE IF NOT EXISTS acolhimento (
    id UUID NOT NULL,
    patient_id UUID NOT NULL,
    data_anamnese DATE,
    hora_anamnese TIME,
    observacoes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_acolhimento PRIMARY KEY (id),
    CONSTRAINT uk_acolhimento_patient_id UNIQUE (patient_id)
);

CREATE INDEX IF NOT EXISTS idx_acolhimento_patient_id
    ON acolhimento (patient_id);

CREATE TABLE IF NOT EXISTS acolhimento_avaliacao (
    id UUID NOT NULL,
    patient_id UUID NOT NULL,
    avaliacao_paciente_id UUID NOT NULL,
    titulo_avaliacao VARCHAR(255) NOT NULL,
    modalidade VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_acolhimento_avaliacao PRIMARY KEY (id),
    CONSTRAINT uk_acolhimento_avaliacao_patient_avaliacao
        UNIQUE (patient_id, avaliacao_paciente_id)
);

CREATE INDEX IF NOT EXISTS idx_acolhimento_avaliacao_patient_id
    ON acolhimento_avaliacao (patient_id);

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    correlation_id UUID NOT NULL,
    producer_service VARCHAR(255) NOT NULL,
    exchange_name VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_version SMALLINT NOT NULL,
    payload TEXT NOT NULL,
    actor_id UUID,
    user_agent VARCHAR(255),
    origin_ip VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    attempt_count INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    next_retry_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    locked_by UUID,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id),
    CONSTRAINT ck_outbox_event_version CHECK (event_version > 0),
    CONSTRAINT ck_outbox_status CHECK (status IN ('NEW', 'LOCKED', 'PUBLISHED', 'FAILED', 'DEAD')),
    CONSTRAINT ck_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_outbox_max_attempts CHECK (max_attempts > 0)
);

CREATE INDEX IF NOT EXISTS idx_outbox_dispatch ON outbox_events (status, next_retry_at, created_at, id);
CREATE INDEX IF NOT EXISTS idx_outbox_correlation ON outbox_events (correlation_id);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id, created_at);

CREATE OR REPLACE VIEW outbox_event AS SELECT * FROM outbox_events;

CREATE TABLE IF NOT EXISTS processed_events (
    id BIGSERIAL PRIMARY KEY,
    consumer_name VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    event_hash CHAR(64),
    response_event_id UUID,
    correlation_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    actor_id UUID,
    user_agent VARCHAR(255),
    origin_ip VARCHAR(255),
    processed_at TIMESTAMPTZ NOT NULL,
    result VARCHAR(255) NOT NULL,
    reason_code VARCHAR(255),
    error_message TEXT,
    CONSTRAINT uk_processed_consumer_event UNIQUE (consumer_name, event_id),
    CONSTRAINT ck_processed_result CHECK (result IN ('SUCCESS', 'IGNORED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_processed_correlation ON processed_events (correlation_id);
CREATE INDEX IF NOT EXISTS idx_processed_event ON processed_events (event_id);
CREATE INDEX IF NOT EXISTS idx_processed_time ON processed_events (processed_at, id);
CREATE INDEX IF NOT EXISTS idx_processed_reason_code ON processed_events (reason_code);

CREATE OR REPLACE VIEW processed_event AS SELECT * FROM processed_events;

COMMIT;
