use crate::application::http::dto::{
    AcolhimentoCreateDto, AcolhimentoPatientStatusDto, AcolhimentoRetrieveDto, AcolhimentoUpdateDto,
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
) -> Result<SuccessResponseDto<AcolhimentoRetrieveDto>, HttpError> {
    state
        .acolhimento_service
        .find_by_patient_id(patient_id)
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_RETRIEVED",
                context.correlation_id.to_string(),
                "Acolhimento recuperado com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn status_by_patient_id(
    State(state): State<ApplicationState>,
    ApiPath(patient_id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
) -> Result<SuccessResponseDto<AcolhimentoPatientStatusDto>, HttpError> {
    state
        .acolhimento_service
        .find_status_by_patient_id(patient_id)
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_STATUS_RETRIEVED",
                context.correlation_id.to_string(),
                "Status de acolhimento recuperado com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn create(
    State(state): State<ApplicationState>,
    Extension(context): Extension<RequestContext>,
    ApiJson(dto): ApiJson<AcolhimentoCreateDto>,
) -> Result<SuccessResponseDto<AcolhimentoRetrieveDto>, HttpError> {
    state
        .acolhimento_service
        .create(
            dto,
            context.actor_id,
            context.correlation_id,
            context.user_agent.clone(),
            context.origin_ip.clone(),
        )
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_CREATED",
                context.correlation_id.to_string(),
                "Acolhimento registrado com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn update(
    State(state): State<ApplicationState>,
    ApiPath(id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
    ApiJson(dto): ApiJson<AcolhimentoUpdateDto>,
) -> Result<SuccessResponseDto<AcolhimentoRetrieveDto>, HttpError> {
    state
        .acolhimento_service
        .update_by_patient_id(
            id,
            dto,
            context.actor_id,
            context.correlation_id,
            context.user_agent.clone(),
            context.origin_ip.clone(),
        )
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_UPDATED",
                context.correlation_id.to_string(),
                "Acolhimento atualizado com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn delete(
    State(state): State<ApplicationState>,
    ApiPath(patient_id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
) -> Result<SuccessResponseDto<()>, HttpError> {
    state
        .acolhimento_service
        .delete_by_patient_id(
            patient_id,
            context.actor_id,
            context.correlation_id,
            context.user_agent.clone(),
            context.origin_ip.clone(),
        )
        .await
        .map(|()| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_DELETED",
                context.correlation_id.to_string(),
                "Acolhimento removido com sucesso",
                (),
            )
        })
        .map_err(|error| HttpError::new(error, context))
}
