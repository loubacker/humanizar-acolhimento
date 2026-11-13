use axum::body::{Body, to_bytes};
use axum::extract::{MatchedPath, Request, State};
use axum::http::{Method, StatusCode, header};
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use sha2::{Digest, Sha256};
use uuid::Uuid;

use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{
    OperationAction, OperationIdentity, OperationReservation, OperationState,
};
use crate::infrastructure::controller::handler::{ErrorObservation, HttpError};
use crate::infrastructure::controller::{ApplicationState, RequestContext};

const MAX_BODY: usize = 2 * 1024 * 1024;

pub(crate) async fn guard(
    State((state, body_limit)): State<(ApplicationState, usize)>,
    request: Request,
    next: Next,
) -> Response {
    match reserve_and_execute(state, body_limit, request, next).await {
        Ok(response) => response,
        Err(error) => error.into_response(),
    }
}

async fn reserve_and_execute(
    state: ApplicationState,
    body_limit: usize,
    request: Request,
    next: Next,
) -> Result<Response, HttpError> {
    let context = RequestContext::from_request(&request);
    let responses = state
        .operation_responses
        .clone()
        .ok_or_else(|| HttpError::reason(ReasonCode::PersistenceFailure, context.clone()))?;
    let actor = context
        .actor_id
        .ok_or_else(|| HttpError::reason(ReasonCode::AuthorizationDenied, context.clone()))?;
    let key = request
        .headers()
        .get("idempotency-key")
        .and_then(|value| value.to_str().ok())
        .and_then(|value| Uuid::parse_str(value).ok())
        .filter(|value| !value.is_nil())
        .ok_or_else(|| HttpError::reason(ReasonCode::IdempotencyKeyInvalid, context.clone()))?;
    let path = request.uri().path().to_owned();
    let matched = request
        .extensions()
        .get::<MatchedPath>()
        .map(|path| path.as_str().to_owned())
        .ok_or_else(|| HttpError::reason(ReasonCode::RouteNotFound, context.clone()))?;
    let action = action(request.method(), &matched)
        .ok_or_else(|| HttpError::reason(ReasonCode::MethodNotAllowed, context.clone()))?;
    let (mut parts, body) = request.into_parts();
    let bytes = to_bytes(body, body_limit)
        .await
        .map_err(|_| HttpError::reason(ReasonCode::PayloadTooLarge, context.clone()))?;
    let hash = request_hash(action, &path, &matched, &bytes)
        .map_err(|_| HttpError::reason(ReasonCode::RequestBodyInvalid, context.clone()))?;
    let identity = OperationIdentity {
        id: key,
        actor_id: actor,
        action,
        resource: path,
        request_hash: hash,
        owner: Uuid::new_v4(),
    };
    let reservation = state
        .operation_port
        .reserve(&identity)
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    match reservation {
        OperationReservation::Pending => {
            return Err(HttpError::reason(ReasonCode::OperationInProgress, context));
        }
        OperationReservation::Completed => {
            return match responses
                .snapshot(&identity, OperationState::Completed)
                .await
                .map_err(|error| HttpError::new(error, context.clone()))?
            {
                Some(snapshot) => replay(snapshot, &context),
                None => Err(HttpError::reason(ReasonCode::OperationCompleted, context)),
            };
        }
        OperationReservation::Failed => {
            return match responses
                .snapshot(&identity, OperationState::Failed)
                .await
                .map_err(|error| HttpError::new(error, context.clone()))?
            {
                Some(snapshot) => replay(snapshot, &context),
                None => Err(HttpError::reason(ReasonCode::OperationFailed, context)),
            };
        }
        OperationReservation::Acquired => {}
    }
    parts.extensions.insert(identity.clone());
    let response = next
        .run(Request::from_parts(parts, Body::from(bytes)))
        .await;
    let reason = response
        .extensions()
        .get::<ErrorObservation>()
        .map(|observation| observation.reason_code.clone())
        .unwrap_or_else(|| "UNEXPECTED_ERROR".to_owned());
    let (parts, body) = response.into_parts();
    let body = to_bytes(body, MAX_BODY)
        .await
        .map_err(|_| HttpError::reason(ReasonCode::UnexpectedError, context.clone()))?;
    let result = if parts.status.is_success() {
        responses
            .record_response(
                &identity,
                OperationState::Completed,
                parts.status.as_u16(),
                &body,
            )
            .await
    } else {
        match state.operation_port.fail(&identity, &reason).await {
            Ok(()) => {
                responses
                    .record_response(
                        &identity,
                        OperationState::Failed,
                        parts.status.as_u16(),
                        &body,
                    )
                    .await
            }
            Err(error) => Err(error),
        }
    };
    if result.is_err() {
        tracing::error!(service = "humanizar-acolhimento", phase = "operation_response", outcome = "persistence_failure", operation_id = %identity.id);
        if parts.status.is_success() {
            return Err(HttpError::reason(ReasonCode::PersistenceFailure, context));
        }
    }
    Ok(Response::from_parts(parts, Body::from(body)))
}

fn action(method: &Method, path: &str) -> Option<OperationAction> {
    match (method, path) {
        (&Method::POST, "/api/v1/acolhimento/register") => Some(OperationAction::AcolhimentoCriar),
        (&Method::PUT, "/api/v1/acolhimento/update/{id}") => {
            Some(OperationAction::AcolhimentoAtualizar)
        }
        (&Method::DELETE, "/api/v1/acolhimento/delete/{patientId}") => {
            Some(OperationAction::AcolhimentoExcluir)
        }
        (&Method::POST, "/api/v1/acolhimento/anamnese/register")
        | (&Method::POST, "/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}") => {
            Some(OperationAction::AnamneseRegistrar)
        }
        (&Method::PUT, "/api/v1/acolhimento/anamnese/update/{patientId}")
        | (&Method::PUT, "/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}") => {
            Some(OperationAction::AnamneseAtualizar)
        }
        (&Method::DELETE, "/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}") => {
            Some(OperationAction::AnamneseExcluir)
        }
        (&Method::POST, "/api/v1/acolhimento/avaliacoes/register") => {
            Some(OperationAction::AvaliacaoVincular)
        }
        (&Method::DELETE, "/api/v1/acolhimento/avaliacoes/{patientId}") => {
            Some(OperationAction::AvaliacaoDesvincular)
        }
        _ => None,
    }
}

fn request_hash(
    action: OperationAction,
    path: &str,
    matched: &str,
    bytes: &[u8],
) -> Result<String, serde_json::Error> {
    let value: serde_json::Value = if bytes.is_empty() {
        serde_json::Value::Null
    } else {
        serde_json::from_slice(bytes)?
    };
    let business = if matched == "/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}" {
        value.get("payload").unwrap_or(&value)
    } else {
        &value
    };
    let canonical_business = if matches!(
        action,
        OperationAction::AcolhimentoExcluir | OperationAction::AvaliacaoDesvincular
    ) {
        serde_json::Value::Null
    } else if action == OperationAction::AcolhimentoAtualizar {
        let mut canonical = business.clone();
        if let Some(ids) = canonical
            .get_mut("expectedNucleoPatientIds")
            .and_then(serde_json::Value::as_array_mut)
        {
            ids.sort_by_key(|id| id.as_str().unwrap_or_default().to_owned());
        }
        canonical
    } else if action == OperationAction::AvaliacaoVincular {
        serde_json::json!({
            "patientId": business.get("patientId"),
            "avaliacaoPacienteId": business.get("avaliacaoPacienteId"),
        })
    } else {
        business.clone()
    };
    let canonical = serde_json::to_vec(&(action.as_str(), path, canonical_business))?;
    Ok(hex::encode(Sha256::digest(canonical)))
}

fn replay(snapshot: (u16, Vec<u8>), context: &RequestContext) -> Result<Response, HttpError> {
    let status = StatusCode::from_u16(snapshot.0)
        .map_err(|_| HttpError::reason(ReasonCode::PersistenceFailure, context.clone()))?;
    let mut body: serde_json::Value = serde_json::from_slice(&snapshot.1)
        .map_err(|_| HttpError::reason(ReasonCode::PersistenceFailure, context.clone()))?;
    if let Some(object) = body.as_object_mut() {
        object.insert(
            "correlationId".to_owned(),
            serde_json::Value::String(context.correlation_id.to_string()),
        );
    }
    let reason = body
        .get("reasonCode")
        .and_then(serde_json::Value::as_str)
        .unwrap_or("OPERATION_REPLAY")
        .to_owned();
    let code = body
        .get("code")
        .and_then(serde_json::Value::as_str)
        .unwrap_or("OPERATION_REPLAY")
        .to_owned();
    let bytes = serde_json::to_vec(&body)
        .map_err(|_| HttpError::reason(ReasonCode::PersistenceFailure, context.clone()))?;
    let mut response = Response::builder()
        .status(status)
        .header(header::CONTENT_TYPE, "application/json")
        .header(header::CACHE_CONTROL, "no-store")
        .body(Body::from(bytes))
        .map_err(|_| HttpError::reason(ReasonCode::UnexpectedError, context.clone()))?;
    if status.is_client_error() || status.is_server_error() {
        response.extensions_mut().insert(ErrorObservation {
            reason_code: reason,
            source_present: false,
        });
    } else {
        response
            .extensions_mut()
            .insert(crate::infrastructure::controller::dto::SuccessObservation { code });
    }
    Ok(response)
}
