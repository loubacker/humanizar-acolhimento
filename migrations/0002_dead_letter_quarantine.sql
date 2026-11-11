BEGIN;

CREATE TABLE dead_letter_quarantine (
    id UUID NOT NULL,
    queue_name VARCHAR(255) NOT NULL,
    exchange_name VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    payload BYTEA NOT NULL,
    properties BYTEA NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dead_letter_quarantine PRIMARY KEY (id),
    CONSTRAINT ck_dead_letter_quarantine_queue_name CHECK (queue_name <> '')
);

CREATE INDEX idx_dead_letter_quarantine_queue_received
    ON dead_letter_quarantine (queue_name, received_at, id);

COMMIT;
