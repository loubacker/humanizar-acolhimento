-- Adiciona o status de entrega da anamnese em nucleo_patient.
-- Rodar ANTES do deploy do build novo (ddl-auto: update nao adiciona NOT NULL
-- com backfill de forma segura em tabela ja populada).
-- Backfill: ENTREGUE onde existe linha de anamnese (tabela acolhimento), PENDENTE no restante.

BEGIN;

ALTER TABLE nucleo_patient ADD COLUMN IF NOT EXISTS anamnese_status varchar(20);

UPDATE nucleo_patient np
SET anamnese_status = 'ENTREGUE'
WHERE EXISTS (
    SELECT 1 FROM acolhimento a WHERE a.nucleo_patient_id = np.id
);

UPDATE nucleo_patient
SET anamnese_status = 'PENDENTE'
WHERE anamnese_status IS NULL;

ALTER TABLE nucleo_patient ALTER COLUMN anamnese_status SET DEFAULT 'PENDENTE';
ALTER TABLE nucleo_patient ALTER COLUMN anamnese_status SET NOT NULL;

COMMIT;
