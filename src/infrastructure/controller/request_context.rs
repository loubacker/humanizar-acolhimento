use axum::extract::Request;
use axum::http::header;
use uuid::Uuid;

use crate::infrastructure::security::AuthenticatedUser;

#[derive(Debug, Clone)]
pub struct RequestContext {
    pub correlation_id: Uuid,
    pub path: String,
    pub actor_id: Option<Uuid>,
    pub user_agent: Option<String>,
    pub origin_ip: Option<String>,
}

impl RequestContext {
    pub(crate) fn from_request(request: &Request) -> Self {
        if let Some(context) = request.extensions().get::<Self>() {
            return context.clone();
        }
        let correlation_id = request
            .headers()
            .get("x-correlation-id")
            .and_then(|value| value.to_str().ok())
            .and_then(|value| Uuid::parse_str(value).ok())
            .unwrap_or_else(Uuid::new_v4);

        let actor_id = request
            .extensions()
            .get::<AuthenticatedUser>()
            .and_then(|user| Uuid::parse_str(user.subject()).ok());

        let user_agent = request
            .headers()
            .get(header::USER_AGENT)
            .and_then(|value| value.to_str().ok())
            .map(|s| s.chars().take(255).collect());

        let origin_ip = request
            .headers()
            .get("x-forwarded-for")
            .or_else(|| request.headers().get("x-real-ip"))
            .and_then(|value| value.to_str().ok())
            .map(|s| {
                s.split(',')
                    .next()
                    .unwrap_or("")
                    .trim()
                    .chars()
                    .take(255)
                    .collect()
            });

        Self {
            correlation_id,
            path: request.uri().path().to_owned(),
            actor_id,
            user_agent,
            origin_ip,
        }
    }
}
