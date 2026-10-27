use super::{AuthenticatedUser, JwtValidator};
use crate::domain::exception::SecurityException;
use crate::infrastructure::controller::RequestContext;
use crate::infrastructure::controller::handler::HttpError;
use axum::extract::{Request, State};
use axum::http::header::AUTHORIZATION;
use axum::middleware::Next;
use axum::response::Response;
use std::sync::Arc;

pub(crate) async fn authenticate(
    State(validator): State<Arc<JwtValidator>>,
    mut request: Request,
    next: Next,
) -> Result<Response, HttpError> {
    let context = RequestContext::from_request(&request);
    let token = bearer_token(&request).map_err(|error| HttpError::new(error, context.clone()))?;
    let principal = validator.validate(token).await.map_err(|error| {
        HttpError::new(
            SecurityException::authentication_with_source(error),
            context,
        )
    })?;
    request.extensions_mut().insert(principal);
    Ok(next.run(request).await)
}

pub(crate) async fn require_administrator(
    request: Request,
    next: Next,
) -> Result<Response, HttpError> {
    let context = RequestContext::from_request(&request);
    let principal = request
        .extensions()
        .get::<AuthenticatedUser>()
        .ok_or_else(|| HttpError::new(SecurityException::authentication(), context.clone()))?;
    if !principal.has_role("ADMINISTRADOR") {
        return Err(HttpError::new(SecurityException::authorization(), context));
    }
    Ok(next.run(request).await)
}

fn bearer_token(request: &Request) -> Result<&str, SecurityException> {
    let value = request
        .headers()
        .get(AUTHORIZATION)
        .ok_or_else(SecurityException::authentication)?
        .to_str()
        .map_err(SecurityException::authentication_with_source)?;
    if value.len() > 16384 {
        return Err(SecurityException::authentication());
    }
    let mut parts = value.split_whitespace();
    let scheme = parts.next().unwrap_or_default();
    let token = parts.next().unwrap_or_default();
    if !scheme.eq_ignore_ascii_case("Bearer") || token.is_empty() || parts.next().is_some() {
        return Err(SecurityException::authentication());
    }
    Ok(token)
}
