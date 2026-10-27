use crate::application::http::dto::{
    AcolhimentoAnamneseCreateUpdateDto, AcolhimentoAnamneseRetrieveDto,
};
use crate::infrastructure::controller::dto::SuccessResponseDto;
use crate::infrastructure::controller::extractor::{ApiJson, ApiPath};
use crate::infrastructure::controller::handler::HttpError;
use crate::infrastructure::controller::{ApplicationState, RequestContext};
use axum::Extension;
use axum::extract::State;
use uuid::Uuid;

pub async fn find_by_patient_id(
    State(state): State<ApplicationState>,
    ApiPath(patient_id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
) -> Result<SuccessResponseDto<AcolhimentoAnamneseRetrieveDto>, HttpError> {
    state
        .acolhimento_anamnese_service
        .find_by_patient_id(patient_id)
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_ANAMNESE_RETRIEVED",
                context.correlation_id.to_string(),
                "Anamnese de acolhimento recuperada com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn create(
    State(state): State<ApplicationState>,
    Extension(context): Extension<RequestContext>,
    ApiJson(dto): ApiJson<AcolhimentoAnamneseCreateUpdateDto>,
) -> Result<SuccessResponseDto<AcolhimentoAnamneseRetrieveDto>, HttpError> {
    state
        .acolhimento_anamnese_service
        .upsert(dto)
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_ANAMNESE_CREATED",
                context.correlation_id.to_string(),
                "Anamnese de acolhimento registrada com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn update(
    State(state): State<ApplicationState>,
    ApiPath(patient_id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
    ApiJson(mut dto): ApiJson<AcolhimentoAnamneseCreateUpdateDto>,
) -> Result<SuccessResponseDto<AcolhimentoAnamneseRetrieveDto>, HttpError> {
    dto.patient_id = Some(patient_id);
    state
        .acolhimento_anamnese_service
        .upsert(dto)
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_ANAMNESE_UPDATED",
                context.correlation_id.to_string(),
                "Anamnese de acolhimento atualizada com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}
