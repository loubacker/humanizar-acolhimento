use super::acolhimento::{
    acolhimento_anamnese_controller, acolhimento_avaliacao_controller, acolhimento_controller,
    acolhimento_history_controller, acolhimento_operation_controller,
};
use super::{ApplicationState, http_observability, operation_guard};
use crate::infrastructure::config::SecurityConfig;
use axum::extract::{DefaultBodyLimit, State};
use axum::http::{HeaderValue, Method, StatusCode};
use axum::response::{IntoResponse, Response};
use axum::routing::{delete, get, post, put};
use axum::{Extension, Json, Router, middleware};
use std::sync::atomic::Ordering;
use std::time::Duration;
use tower_http::cors::{AllowOrigin, CorsLayer};
use url::Url;

pub fn create_router(state: ApplicationState, security: &SecurityConfig) -> Router {
    create_router_with_limits(state, security, 2 * 1024 * 1024, Duration::from_secs(30))
}

pub fn create_router_with_limits(
    state: ApplicationState,
    security: &SecurityConfig,
    body_limit: usize,
    timeout: Duration,
) -> Router {
    let health_state = state.clone();
    let operations = security.protect_administrator(
        Router::new()
            .route(
                "/api/v1/acolhimento/paciente/{patientId}/historico",
                get(acolhimento_history_controller::list),
            )
            .route(
                "/api/v1/acolhimento/paciente/{patientId}/historico/{nucleoPatientId}",
                get(acolhimento_history_controller::detail),
            )
            .route(
                "/api/v1/acolhimento/operacoes/{operationId}",
                get(acolhimento_operation_controller::find),
            )
            .with_state(state.clone()),
    );
    let reads = security.protect_authenticated(
        Router::new()
            .route(
                "/api/v1/acolhimento/paciente/{patientId}",
                get(acolhimento_controller::find_by_patient_id),
            )
            .route(
                "/api/v1/acolhimento/paciente/status/{patientId}",
                get(acolhimento_controller::status_by_patient_id),
            )
            .route(
                "/api/v1/acolhimento/anamnese/paciente/{patientId}",
                get(acolhimento_anamnese_controller::find_by_patient_id),
            )
            .route(
                "/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}",
                get(acolhimento_anamnese_controller::find_by_patient_and_nucleo),
            )
            .route(
                "/api/v1/acolhimento/avaliacoes/{patientId}",
                get(acolhimento_avaliacao_controller::find_by_patient_id),
            )
            .route(
                "/api/v1/acolhimento/avaliacoes-disponiveis/{patientId}",
                get(acolhimento_avaliacao_controller::find_available),
            )
            .with_state(state.clone()),
    );
    let writes = security.protect_administrator(
        Router::new()
            .route(
                "/api/v1/acolhimento/register",
                post(acolhimento_controller::create),
            )
            .route(
                "/api/v1/acolhimento/update/{id}",
                put(acolhimento_controller::update),
            )
            .route(
                "/api/v1/acolhimento/delete/{patientId}",
                delete(acolhimento_controller::delete),
            )
            .route(
                "/api/v1/acolhimento/anamnese/register",
                post(acolhimento_anamnese_controller::create),
            )
            .route(
                "/api/v1/acolhimento/anamnese/update/{patientId}",
                put(acolhimento_anamnese_controller::update),
            )
            .route(
                "/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}",
                post(acolhimento_anamnese_controller::create_for_nucleo)
                    .put(acolhimento_anamnese_controller::update_for_nucleo)
                    .delete(acolhimento_anamnese_controller::delete_for_nucleo),
            )
            .route(
                "/api/v1/acolhimento/avaliacoes/register",
                post(acolhimento_avaliacao_controller::create),
            )
            .route(
                "/api/v1/acolhimento/avaliacoes/{patientId}",
                delete(acolhimento_avaliacao_controller::delete),
            )
            .route_layer(middleware::from_fn_with_state(
                (state.clone(), body_limit),
                operation_guard::guard,
            ))
            .with_state(state),
    );
    let router = reads
        .merge(writes)
        .merge(operations)
        .merge(
            Router::new()
                .route("/health", get(health))
                .with_state(health_state),
        )
        .fallback(StatusCode::NOT_FOUND)
        .layer(DefaultBodyLimit::max(body_limit));
    http_observability::apply(router, timeout, cors())
}

async fn health(
    State(state): State<ApplicationState>,
    Extension(context): Extension<super::RequestContext>,
) -> Response {
    if !state.messaging_ready.load(Ordering::Acquire) {
        return super::handler::HttpError::reason(
            crate::domain::model::enums::ReasonCode::MessagingUnavailable,
            context,
        )
        .into_response();
    }
    if let Some(database) = &state.database {
        let probe = async {
            let client = database
                .acquire()
                .await
                .map_err(crate::domain::exception::AcolhimentoException::from)?;
            client.query_one("SELECT 1", &[]).await.map_err(|error| {
                crate::domain::exception::AcolhimentoException::with_source(
                    crate::domain::model::enums::ReasonCode::PersistenceFailure,
                    error,
                )
            })?;
            Ok::<(), crate::domain::exception::AcolhimentoException>(())
        };
        match tokio::time::timeout(Duration::from_secs(2), probe).await {
            Ok(Ok(())) => {}
            Ok(Err(error)) => {
                return super::handler::HttpError::new(error, context).into_response();
            }
            Err(error) => {
                return super::handler::HttpError::new(
                    crate::domain::exception::AcolhimentoException::with_source(
                        crate::domain::model::enums::ReasonCode::PersistenceFailure,
                        error,
                    ),
                    context,
                )
                .into_response();
            }
        }
    }
    Json(serde_json::json!({"status":"UP"})).into_response()
}

fn cors() -> CorsLayer {
    CorsLayer::new()
        .allow_origin(AllowOrigin::predicate(|origin: &HeaderValue, _| {
            let Some(url) = origin
                .to_str()
                .ok()
                .and_then(|value| Url::parse(value).ok())
            else {
                return false;
            };
            if url.scheme() != "http" {
                return false;
            }
            match url.host_str() {
                Some("localhost" | "127.0.0.1") => true,
                Some(host) => host
                    .parse::<std::net::Ipv4Addr>()
                    .is_ok_and(|address| address.octets()[0] == 10),
                None => false,
            }
        }))
        .allow_methods([
            Method::GET,
            Method::POST,
            Method::PUT,
            Method::DELETE,
            Method::PATCH,
            Method::OPTIONS,
        ])
        .allow_headers(
            [
                "authorization",
                "content-type",
                "accept",
                "origin",
                "x-requested-with",
                "x-correlation-id",
                "idempotency-key",
                "x-page-visited",
                "x-csrf-token",
            ]
            .map(axum::http::HeaderName::from_static),
        )
        .expose_headers(
            ["authorization", "x-correlation-id"].map(axum::http::HeaderName::from_static),
        )
        .allow_credentials(true)
        .max_age(Duration::from_secs(3600))
}
