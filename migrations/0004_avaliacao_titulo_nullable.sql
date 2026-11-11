BEGIN;

ALTER TABLE acolhimento_avaliacao ALTER COLUMN titulo_avaliacao DROP NOT NULL;

COMMIT;
