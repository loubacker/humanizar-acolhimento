use crate::domain::exception::TechnicalError;
use crate::infrastructure::config::rabbit::RabbitSettings;
use crate::infrastructure::config::{
    DatabaseConfig, DatabaseSettings, EnvironmentConfig, SecurityConfig, SecuritySettings,
    ServerConfig,
};
use crate::infrastructure::controller::{ApplicationState, create_router_with_limits};
use crate::infrastructure::messaging::inbound::rabbit::RabbitRuntime;

pub async fn run() -> Result<(), TechnicalError> {
    EnvironmentConfig::load()?;
    tracing_subscriber::fmt()
        .with_target(false)
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()),
        )
        .try_init()
        .map_err(|_| TechnicalError::new("Falha ao inicializar tracing"))?;
    let server = ServerConfig::from_env()?;
    let body_limit =
        EnvironmentConfig::positive_u32("HTTP_BODY_LIMIT_BYTES", 2 * 1024 * 1024)? as usize;
    let timeout = EnvironmentConfig::positive_duration_seconds("HTTP_TIMEOUT_SECONDS", 30)?;
    let messaging_settings = RabbitSettings::from_env()?;
    let max_attempts = messaging_settings.max_attempts;
    let database = DatabaseConfig::initialize(DatabaseSettings::from_env()?).await?;
    validate_schema(&database).await?;
    let security = SecurityConfig::initialize(SecuritySettings::from_env()?).await?;
    let listener = tokio::net::TcpListener::bind(server.bind_address())
        .await
        .map_err(|error| TechnicalError::with_source("Falha ao iniciar listener HTTP", error))?;
    let runtime = RabbitRuntime::start(database.clone(), messaging_settings).await?;
    let state = ApplicationState::from_database(database, runtime.readiness(), max_attempts);
    let router = create_router_with_limits(state, &security, body_limit, timeout);
    tracing::info!(
        service = "humanizar-acolhimento",
        phase = "startup",
        outcome = "ready",
        port = server.port()
    );
    let result = axum::serve(listener, router)
        .with_graceful_shutdown(shutdown_signal())
        .await
        .map_err(|error| TechnicalError::with_source("Falha durante execucao HTTP", error));
    let shutdown = runtime.shutdown().await;
    result?;
    shutdown
}

async fn validate_schema(database: &DatabaseConfig) -> Result<(), TechnicalError> {
    let client = database
        .acquire()
        .await
        .map_err(|error| TechnicalError::with_source("Falha ao validar schema", error))?;
    for query in [
        "SELECT id, patient_id, nucleo_id, anamnese, created_at, updated_at FROM nucleo_patient LIMIT 0",
        "SELECT id, nucleo_patient_id, staff_user_id, role FROM nucleo_patient_staff LIMIT 0",
        "SELECT id, patient_id, data_anamnese, hora_anamnese, observacoes, created_at, updated_at FROM acolhimento LIMIT 0",
        "SELECT id, patient_id, avaliacao_paciente_id, titulo_avaliacao, modalidade, status, created_at FROM acolhimento_avaliacao LIMIT 0",
        "SELECT id, event_id, correlation_id, producer_service, exchange_name, routing_key, aggregate_type, aggregate_id, event_version, payload, actor_id, user_agent, origin_ip, status, attempt_count, max_attempts, next_retry_at, last_error, created_at, published_at, locked_by FROM outbox_events LIMIT 0",
        "SELECT id, consumer_name, event_id, event_hash, response_event_id, correlation_id, event_type, aggregate_type, aggregate_id, actor_id, user_agent, origin_ip, processed_at, result, reason_code, error_message FROM processed_events LIMIT 0",
    ] {
        client.query(query, &[]).await.map_err(|error| {
            TechnicalError::with_source(
                "Schema incompativel; aplicar SQL revisado antes do startup",
                error,
            )
        })?;
    }
    let row = client
        .query_one(
            r#"
            SELECT
                EXISTS (
                    SELECT 1 FROM pg_constraint
                    WHERE conrelid = 'nucleo_patient_staff'::regclass
                        AND confrelid = 'nucleo_patient'::regclass
                        AND conname = 'fk_nucleo_patient_staff_nucleo_patient'
                        AND contype = 'f'
                        AND convalidated
                        AND NOT condeferrable
                        AND confdeltype = 'c'
                        AND conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'nucleo_patient_staff'::regclass AND attname = 'nucleo_patient_id')]::SMALLINT[]
                        AND confkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'nucleo_patient'::regclass AND attname = 'id')]::SMALLINT[]
                ) AS valid_fk,
                EXISTS (
                    SELECT 1 FROM pg_index AS table_index
                    WHERE table_index.indrelid = 'outbox_events'::regclass
                        AND table_index.indisunique AND table_index.indisvalid
                        AND table_index.indpred IS NULL AND table_index.indexprs IS NULL
                        AND table_index.indnkeyatts = 1
                        AND (
                            SELECT ARRAY_AGG(attribute.attname ORDER BY key.ordinality)
                            FROM UNNEST(table_index.indkey) WITH ORDINALITY AS key(number, ordinality)
                            JOIN pg_attribute AS attribute ON attribute.attrelid = table_index.indrelid AND attribute.attnum = key.number
                            WHERE key.ordinality <= table_index.indnkeyatts
                        ) = ARRAY['event_id']::NAME[]
                ) AS valid_outbox_unique,
                EXISTS (
                    SELECT 1 FROM pg_index AS table_index
                    WHERE table_index.indrelid = 'processed_events'::regclass
                        AND table_index.indisunique AND table_index.indisvalid
                        AND table_index.indpred IS NULL AND table_index.indexprs IS NULL
                        AND table_index.indnkeyatts = 2
                        AND (
                            SELECT ARRAY_AGG(attribute.attname ORDER BY key.ordinality)
                            FROM UNNEST(table_index.indkey) WITH ORDINALITY AS key(number, ordinality)
                            JOIN pg_attribute AS attribute ON attribute.attrelid = table_index.indrelid AND attribute.attnum = key.number
                            WHERE key.ordinality <= table_index.indnkeyatts
                        ) = ARRAY['consumer_name', 'event_id']::NAME[]
                ) AS valid_processed_unique
            "#,
            &[],
        )
        .await
        .map_err(|error| TechnicalError::with_source("Falha ao validar constraints", error))?;
    for field in ["valid_fk", "valid_outbox_unique", "valid_processed_unique"] {
        if !row
            .try_get::<_, bool>(field)
            .map_err(|error| TechnicalError::with_source("Schema invalido", error))?
        {
            return Err(TechnicalError::new(
                "Constraints do schema incompativeis; revisar SQL versionado",
            ));
        }
    }
    Ok(())
}

async fn shutdown_signal() {
    #[cfg(unix)]
    {
        if let Ok(mut terminate) =
            tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
        {
            tokio::select! { _ = tokio::signal::ctrl_c() => {}, _ = terminate.recv() => {} }
            return;
        }
    }
    if tokio::signal::ctrl_c().await.is_err() {
        tracing::error!(
            service = "humanizar-acolhimento",
            phase = "shutdown_signal",
            outcome = "failure"
        );
    }
}
