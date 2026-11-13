BEGIN;

ALTER TABLE nucleo_patient
    ADD COLUMN desvinculado_at TIMESTAMPTZ,
    ADD COLUMN desvinculado_por UUID,
    ADD COLUMN desvinculo_operation_id UUID,
    ADD CONSTRAINT ck_nucleo_patient_desvinculo CHECK (
        (desvinculado_at IS NULL AND desvinculado_por IS NULL AND desvinculo_operation_id IS NULL)
        OR (desvinculado_at IS NOT NULL AND desvinculado_por IS NOT NULL AND desvinculo_operation_id IS NOT NULL)
    );

ALTER TABLE nucleo_patient DROP CONSTRAINT uk_nucleo_patient_patient_nucleo;

CREATE UNIQUE INDEX uk_nucleo_patient_patient_nucleo
    ON nucleo_patient (patient_id, nucleo_id)
    WHERE desvinculado_at IS NULL;

CREATE INDEX idx_nucleo_patient_historico
    ON nucleo_patient (patient_id, desvinculado_at DESC, id DESC)
    WHERE desvinculado_at IS NOT NULL;

COMMIT;
