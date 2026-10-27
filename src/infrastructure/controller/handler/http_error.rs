use crate::domain::exception::{AcolhimentoException, ReasonCodeException};
use crate::domain::model::enums::ReasonCode;
use crate::infrastructure::controller::RequestContext;
use crate::infrastructure::controller::dto::ErrorResponseDto;
use axum::Json;
use axum::http::{StatusCode, header};
use axum::response::{IntoResponse, Response};
use chrono::Utc;

pub struct HttpError {
    exception: Box<dyn ReasonCodeException>,
    context: Box<RequestContext>,
}

#[derive(Clone)]
pub(crate) struct ErrorObservation {
    pub reason_code: &'static str,
    pub source_present: bool,
}

impl HttpError {
    pub fn new(exception: impl ReasonCodeException + 'static, context: RequestContext) -> Self {
        Self {
            exception: Box::new(exception),
            context: Box::new(context),
        }
    }

    pub(crate) fn reason(reason: ReasonCode, context: RequestContext) -> Self {
        Self::new(AcolhimentoException::new(reason), context)
    }
}

impl IntoResponse for HttpError {
    fn into_response(self) -> Response {
        let reason = self.exception.reason_code();
        let status = status_for(reason);
        let dto = ErrorResponseDto {
            timestamp: Utc::now(),
            status: status.as_u16(),
            error: status
                .canonical_reason()
                .unwrap_or("Internal Server Error")
                .to_owned(),
            reason_code: reason.code().to_owned(),
            message: self.exception.message().to_owned(),
            path: self.context.path,
            correlation_id: self.context.correlation_id.to_string(),
        };
        let mut response = (status, Json(dto)).into_response();
        response.extensions_mut().insert(ErrorObservation {
            reason_code: reason.code(),
            source_present: self.exception.source().is_some(),
        });
        response.headers_mut().insert(
            header::CACHE_CONTROL,
            header::HeaderValue::from_static("no-store"),
        );
        if status == StatusCode::UNAUTHORIZED {
            response.headers_mut().insert(
                header::WWW_AUTHENTICATE,
                header::HeaderValue::from_static("Bearer"),
            );
        }
        response
    }
}

pub const fn status_for(reason: ReasonCode) -> StatusCode {
    match reason {
        ReasonCode::PayloadRequired
        | ReasonCode::RequestBodyInvalid
        | ReasonCode::RequestParameterMissing
        | ReasonCode::RequestParameterInvalid
        | ReasonCode::ValidationError
        | ReasonCode::PatientIdRequired
        | ReasonCode::NucleoPatientEmpty
        | ReasonCode::NucleoStaffEmpty
        | ReasonCode::StaffUserIdRequired
        | ReasonCode::RoleRequired
        | ReasonCode::AvaliacaoPacienteIdRequired
        | ReasonCode::TituloAvaliacaoRequired
        | ReasonCode::InboundRequiredField
        | ReasonCode::InboundParseError
        | ReasonCode::UnsupportedRoutingKey => StatusCode::BAD_REQUEST,
        ReasonCode::AuthenticationRequired => StatusCode::UNAUTHORIZED,
        ReasonCode::AuthorizationDenied => StatusCode::FORBIDDEN,
        ReasonCode::AcolhimentoNotFound
        | ReasonCode::NucleoPatientNotFound
        | ReasonCode::AcolhimentoAvaliacaoNotFound
        | ReasonCode::RouteNotFound => StatusCode::NOT_FOUND,
        ReasonCode::AcolhimentoAlreadyExists
        | ReasonCode::AcolhimentoAvaliacaoAlreadyExists
        | ReasonCode::DuplicateEvent => StatusCode::CONFLICT,
        ReasonCode::UnsupportedEventVersion => StatusCode::UNPROCESSABLE_ENTITY,
        ReasonCode::DatabaseAccessFailed | ReasonCode::UnexpectedError => {
            StatusCode::INTERNAL_SERVER_ERROR
        }
        ReasonCode::IntegrationFailure => StatusCode::BAD_GATEWAY,
        ReasonCode::PersistenceFailure | ReasonCode::MessagingUnavailable => {
            StatusCode::SERVICE_UNAVAILABLE
        }
        ReasonCode::MethodNotAllowed => StatusCode::METHOD_NOT_ALLOWED,
        ReasonCode::PayloadTooLarge => StatusCode::PAYLOAD_TOO_LARGE,
        ReasonCode::RequestTimeout => StatusCode::GATEWAY_TIMEOUT,
    }
}
