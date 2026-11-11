use crate::application::http::dto::{
    AcolhimentoAvaliacaoRegisterDto, AcolhimentoAvaliacaoRetrieveDto, AvaliacaoDisponivelDto,
};
use crate::domain::model::{DelegatedAccess, OperationIdentity};
use crate::infrastructure::controller::dto::SuccessResponseDto;
use crate::infrastructure::controller::extractor::{ApiJson, ApiPath};
use crate::infrastructure::controller::handler::HttpError;
use crate::infrastructure::controller::{ApplicationState, RequestContext};
use axum::Extension;
use axum::extract::State;
use axum::http::StatusCode;
use uuid::Uuid;

pub async fn find_by_patient_id(
    State(state): State<ApplicationState>,
    ApiPath(patient_id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
    Extension(access): Extension<DelegatedAccess>,
) -> Result<SuccessResponseDto<Vec<AcolhimentoAvaliacaoRetrieveDto>>, HttpError> {
    state
        .acolhimento_avaliacao_service
        .find_by_patient_id(patient_id, &access, context.correlation_id)
        .await
        .map(|data| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_AVALIACOES_RETRIEVED",
                context.correlation_id.to_string(),
                "Avaliações de acolhimento recuperadas com sucesso",
                data,
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn find_available(
    State(state): State<ApplicationState>,
    ApiPath(patient_id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
    Extension(access): Extension<DelegatedAccess>,
) -> Result<SuccessResponseDto<Vec<AvaliacaoDisponivelDto>>, HttpError> {
    let data = state
        .acolhimento_avaliacao_service
        .find_available(patient_id, &access, context.correlation_id)
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    Ok(SuccessResponseDto::new(
        "AVALIACOES_DISPONIVEIS_RETRIEVED",
        context.correlation_id.to_string(),
        "Avaliações do paciente consultadas com sucesso",
        data,
    ))
}

pub async fn create(
    State(state): State<ApplicationState>,
    Extension(context): Extension<RequestContext>,
    Extension(operation): Extension<OperationIdentity>,
    Extension(access): Extension<DelegatedAccess>,
    ApiJson(dto): ApiJson<AcolhimentoAvaliacaoRegisterDto>,
) -> Result<
    (
        StatusCode,
        SuccessResponseDto<AcolhimentoAvaliacaoRetrieveDto>,
    ),
    HttpError,
> {
    state
        .acolhimento_avaliacao_service
        .create(dto, &operation, &access, context.correlation_id)
        .await
        .map(|data| {
            (
                StatusCode::CREATED,
                SuccessResponseDto::new(
                    "ACOLHIMENTO_AVALIACAO_CREATED",
                    context.correlation_id.to_string(),
                    "Avaliação vinculada ao acolhimento com sucesso",
                    data,
                ),
            )
        })
        .map_err(|error| HttpError::new(error, context))
}

pub async fn delete(
    State(state): State<ApplicationState>,
    ApiPath(id): ApiPath<Uuid>,
    Extension(context): Extension<RequestContext>,
    Extension(operation): Extension<OperationIdentity>,
) -> Result<SuccessResponseDto<()>, HttpError> {
    state
        .acolhimento_avaliacao_service
        .delete_by_id(id, &operation)
        .await
        .map(|()| {
            SuccessResponseDto::new(
                "ACOLHIMENTO_AVALIACAO_DELETED",
                context.correlation_id.to_string(),
                "Vínculo de avaliação removido com sucesso",
                (),
            )
        })
        .map_err(|error| HttpError::new(error, context))
}
