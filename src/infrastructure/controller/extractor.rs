use super::RequestContext;
use super::handler::HttpError;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use axum::Json;
use axum::extract::{FromRequest, FromRequestParts, Path, Request};
use axum::http::{StatusCode, request::Parts};
use serde::de::DeserializeOwned;

pub struct ApiJson<T>(pub T);

impl<T: DeserializeOwned, S: Send + Sync> FromRequest<S> for ApiJson<T> {
    type Rejection = HttpError;

    async fn from_request(request: Request, state: &S) -> Result<Self, Self::Rejection> {
        let context = RequestContext::from_request(&request);
        let Json(value) = Json::<Option<T>>::from_request(request, state)
            .await
            .map_err(|error| {
                let reason = if error.status() == StatusCode::PAYLOAD_TOO_LARGE {
                    ReasonCode::PayloadTooLarge
                } else {
                    ReasonCode::RequestBodyInvalid
                };
                HttpError::new(
                    AcolhimentoException::with_source(reason, error),
                    context.clone(),
                )
            })?;
        value
            .map(Self)
            .ok_or_else(|| HttpError::reason(ReasonCode::PayloadRequired, context))
    }
}

pub struct ApiPath<T>(pub T);

impl<T: DeserializeOwned + Send, S: Send + Sync> FromRequestParts<S> for ApiPath<T> {
    type Rejection = HttpError;

    async fn from_request_parts(parts: &mut Parts, state: &S) -> Result<Self, Self::Rejection> {
        let context = parts
            .extensions
            .get::<RequestContext>()
            .cloned()
            .unwrap_or_else(|| RequestContext {
                correlation_id: uuid::Uuid::new_v4(),
                path: parts.uri.path().to_owned(),
                actor_id: None,
                user_agent: None,
                origin_ip: None,
            });
        Path::<T>::from_request_parts(parts, state)
            .await
            .map(|Path(value)| Self(value))
            .map_err(|error| {
                HttpError::new(
                    AcolhimentoException::with_source(ReasonCode::RequestParameterInvalid, error),
                    context,
                )
            })
    }
}
