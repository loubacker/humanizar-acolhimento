use std::str::FromStr;

use axum::Extension;
use axum::extract::{RawQuery, State};
use serde::Serialize;
use uuid::Uuid;

use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{OperationAction, OperationStatus};
use crate::infrastructure::controller::dto::SuccessResponseDto;
use crate::infrastructure::controller::extractor::ApiPath;
use crate::infrastructure::controller::handler::HttpError;
use crate::infrastructure::controller::{ApplicationState, RequestContext};

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OperationStatusDto {
    operation_id: Uuid,
    acao: &'static str,
    status: &'static str,
    reason_code: Option<String>,
}

impl OperationStatusDto {
    fn from_status(status: OperationStatus) -> Self {
        Self {
            operation_id: status.id,
            acao: status.action.as_str(),
            status: status.state.as_str(),
            reason_code: status.reason_code,
        }
    }
}

pub async fn find(
    State(state): State<ApplicationState>,
    ApiPath(id): ApiPath<Uuid>,
    RawQuery(query): RawQuery,
    Extension(context): Extension<RequestContext>,
) -> Result<SuccessResponseDto<OperationStatusDto>, HttpError> {
    let actor = context
        .actor_id
        .ok_or_else(|| HttpError::reason(ReasonCode::AuthorizationDenied, context.clone()))?;
    let query = query.unwrap_or_default();
    let mut values = url::form_urlencoded::parse(query.as_bytes())
        .filter(|(key, _)| key == "acao")
        .map(|(_, value)| value.into_owned());
    let action = values
        .next()
        .and_then(|value| OperationAction::from_str(&value).ok())
        .filter(|_| values.next().is_none())
        .ok_or_else(|| HttpError::reason(ReasonCode::RequestParameterInvalid, context.clone()))?;
    let operation = state
        .operation_port
        .find(id, actor, action)
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?
        .ok_or_else(|| HttpError::reason(ReasonCode::OperationNotFound, context.clone()))?;
    Ok(SuccessResponseDto::new(
        "ACOLHIMENTO_OPERATION_RETRIEVED",
        context.correlation_id.to_string(),
        "Operacao consultada com sucesso",
        OperationStatusDto::from_status(operation),
    ))
}
