use super::RequestContext;
use super::dto::SuccessObservation;
use super::handler::{ErrorObservation, HttpError};
use crate::domain::model::enums::ReasonCode;
use axum::extract::{MatchedPath, Request, State};
use axum::http::{HeaderValue, StatusCode, header};
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use axum::{Router, middleware};
use std::time::{Duration, Instant};
use tower_http::catch_panic::CatchPanicLayer;
use tower_http::cors::CorsLayer;
use uuid::Uuid;

pub(crate) fn apply(router: Router, timeout: Duration, cors: CorsLayer) -> Router {
    router
        .layer(CatchPanicLayer::custom(|_| {
            StatusCode::INTERNAL_SERVER_ERROR.into_response()
        }))
        .layer(middleware::from_fn_with_state(timeout, normalize))
        .layer(cors)
        .layer(middleware::from_fn(observe))
}

async fn observe(mut request: Request, next: Next) -> Response {
    let context = RequestContext::from_request(&request);
    let correlation_id = context.correlation_id;
    let operation = request
        .extensions()
        .get::<MatchedPath>()
        .map(MatchedPath::as_str)
        .unwrap_or("unmatched")
        .to_owned();
    let method = request.method().to_string();
    let health = context.path == "/health";
    request.extensions_mut().insert(context.clone());
    let started = Instant::now();
    let mut response = next.run(request).await;
    if let Ok(value) = HeaderValue::from_str(&correlation_id.to_string()) {
        response.headers_mut().insert("x-correlation-id", value);
    }
    record_response(
        &response,
        &operation,
        &method,
        correlation_id,
        health,
        started,
    );
    response
}

async fn normalize(State(timeout): State<Duration>, request: Request, next: Next) -> Response {
    let context = RequestContext::from_request(&request);
    let mut response = match tokio::time::timeout(timeout, next.run(request)).await {
        Ok(response) => response,
        Err(_) => HttpError::reason(ReasonCode::RequestTimeout, context.clone()).into_response(),
    };
    if (response.status().is_client_error() || response.status().is_server_error())
        && response.extensions().get::<ErrorObservation>().is_none()
    {
        let reason = match response.status() {
            StatusCode::NOT_FOUND => ReasonCode::RouteNotFound,
            StatusCode::METHOD_NOT_ALLOWED => ReasonCode::MethodNotAllowed,
            StatusCode::PAYLOAD_TOO_LARGE => ReasonCode::PayloadTooLarge,
            _ => ReasonCode::UnexpectedError,
        };
        let allow = response.headers().get(header::ALLOW).cloned();
        response = HttpError::reason(reason, context).into_response();
        if let Some(allow) = allow {
            response.headers_mut().insert(header::ALLOW, allow);
        }
    }
    response
}

fn record_response(
    response: &Response,
    operation: &str,
    method: &str,
    correlation_id: Uuid,
    health: bool,
    started: Instant,
) {
    let observation = response.extensions().get::<ErrorObservation>();
    let reason_code = observation
        .map(|value| value.reason_code.as_str())
        .unwrap_or_else(|| {
            response
                .extensions()
                .get::<SuccessObservation>()
                .map(|value| value.code.as_str())
                .unwrap_or("SUCCESS")
        });
    let source_present = observation.is_some_and(|value| value.source_present);
    let status = response.status().as_u16();
    let elapsed_ms = started.elapsed().as_millis() as u64;
    if response.status().is_server_error() {
        tracing::error!(
            service = "humanizar-acolhimento",
            operation,
            method,
            phase = "http_complete",
            outcome = "failure",
            reason_code,
            %correlation_id,
            status,
            elapsed_ms,
            source_present,
        );
    } else if response.status().is_client_error() {
        tracing::warn!(
            service = "humanizar-acolhimento",
            operation,
            method,
            phase = "http_complete",
            outcome = "rejected",
            reason_code,
            %correlation_id,
            status,
            elapsed_ms,
            source_present,
        );
    } else if !health {
        tracing::info!(
            service = "humanizar-acolhimento",
            operation,
            method,
            phase = "http_complete",
            outcome = "success",
            reason_code,
            %correlation_id,
            status,
            elapsed_ms,
        );
    }
}
