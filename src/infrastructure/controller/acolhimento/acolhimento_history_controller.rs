use axum::Extension;
use axum::extract::{RawQuery, State};
use uuid::Uuid;

use crate::application::http::dto::{NucleoPatientHistoryDetailDto, NucleoPatientHistoryPageDto};
use crate::domain::model::enums::ReasonCode;
use crate::infrastructure::controller::dto::SuccessResponseDto;
use crate::infrastructure::controller::extractor::ApiPath;
use crate::infrastructure::controller::handler::HttpError;
use crate::infrastructure::controller::{ApplicationState, RequestContext};

pub async fn list(
    State(state): State<ApplicationState>,
    ApiPath(patient_id): ApiPath<Uuid>,
    RawQuery(query): RawQuery,
    Extension(context): Extension<RequestContext>,
) -> Result<SuccessResponseDto<NucleoPatientHistoryPageDto>, HttpError> {
    let mut page = None;
    let mut size = None;
    for (key, value) in url::form_urlencoded::parse(query.as_deref().unwrap_or_default().as_bytes())
    {
        let target = match key.as_ref() {
            "page" => &mut page,
            "size" => &mut size,
            _ => {
                return Err(HttpError::reason(
                    ReasonCode::RequestParameterInvalid,
                    context,
                ));
            }
        };
        if target.is_some() {
            return Err(HttpError::reason(
                ReasonCode::RequestParameterInvalid,
                context,
            ));
        }
        *target = Some(value.parse::<u32>().map_err(|_| {
            HttpError::reason(ReasonCode::RequestParameterInvalid, context.clone())
        })?);
    }
    let data = state
        .acolhimento_service
        .history(patient_id, page.unwrap_or(0), size.unwrap_or(20))
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    Ok(SuccessResponseDto::new(
        "ACOLHIMENTO_HISTORICO_RETRIEVED",
        context.correlation_id.to_string(),
        "Histórico de vínculos consultado com sucesso",
        data,
    ))
}

pub async fn detail(
    State(state): State<ApplicationState>,
    ApiPath((patient_id, id)): ApiPath<(Uuid, Uuid)>,
    Extension(context): Extension<RequestContext>,
) -> Result<SuccessResponseDto<NucleoPatientHistoryDetailDto>, HttpError> {
    let data = state
        .acolhimento_service
        .history_detail(patient_id, id)
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    Ok(SuccessResponseDto::new(
        "ACOLHIMENTO_HISTORICO_DETAIL_RETRIEVED",
        context.correlation_id.to_string(),
        "Período do vínculo consultado com sucesso",
        data,
    ))
}
