BEGIN;

CREATE OR REPLACE FUNCTION public.notify_cadastral_cache_change()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    previous_patient UUID;
    current_patient UUID;
BEGIN
    IF TG_OP = 'TRUNCATE' THEN
        PERFORM pg_notify('humanizar_acolhimento_history_cache', '');
        RETURN NULL;
    END IF;

    IF TG_TABLE_NAME = 'nucleo_patient' THEN
        IF TG_OP <> 'INSERT' AND OLD.desvinculado_at IS NOT NULL THEN
            previous_patient := OLD.patient_id;
        END IF;
        IF TG_OP <> 'DELETE' AND NEW.desvinculado_at IS NOT NULL THEN
            current_patient := NEW.patient_id;
        END IF;
    ELSE
        IF TG_OP <> 'INSERT' THEN
            SELECT patient_id INTO previous_patient
            FROM public.nucleo_patient
            WHERE id = OLD.nucleo_patient_id;
        END IF;
        IF TG_OP <> 'DELETE' THEN
            SELECT patient_id INTO current_patient
            FROM public.nucleo_patient
            WHERE id = NEW.nucleo_patient_id;
        END IF;
        IF (TG_OP <> 'INSERT' AND previous_patient IS NULL)
            OR (TG_OP <> 'DELETE' AND current_patient IS NULL) THEN
            PERFORM pg_notify('humanizar_acolhimento_history_cache', '');
            RETURN NULL;
        END IF;
    END IF;

    IF previous_patient IS NOT NULL THEN
        PERFORM pg_notify('humanizar_acolhimento_history_cache', previous_patient::TEXT);
    END IF;
    IF current_patient IS NOT NULL AND current_patient IS DISTINCT FROM previous_patient THEN
        PERFORM pg_notify('humanizar_acolhimento_history_cache', current_patient::TEXT);
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE TRIGGER trg_nucleo_patient_history_cache
    AFTER INSERT OR UPDATE OR DELETE ON public.nucleo_patient
    FOR EACH ROW EXECUTE FUNCTION public.notify_cadastral_cache_change();

CREATE OR REPLACE TRIGGER trg_nucleo_patient_staff_history_cache
    AFTER INSERT OR UPDATE OR DELETE ON public.nucleo_patient_staff
    FOR EACH ROW EXECUTE FUNCTION public.notify_cadastral_cache_change();

CREATE OR REPLACE TRIGGER trg_nucleo_patient_history_cache_truncate
    AFTER TRUNCATE ON public.nucleo_patient
    FOR EACH STATEMENT EXECUTE FUNCTION public.notify_cadastral_cache_change();

CREATE OR REPLACE TRIGGER trg_nucleo_patient_staff_history_cache_truncate
    AFTER TRUNCATE ON public.nucleo_patient_staff
    FOR EACH STATEMENT EXECUTE FUNCTION public.notify_cadastral_cache_change();

COMMIT;
