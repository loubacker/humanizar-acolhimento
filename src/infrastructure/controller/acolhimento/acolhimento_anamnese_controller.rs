use crate::application::http::dto::{
    AcolhimentoAnamneseCreateUpdateDto, AcolhimentoAnamneseRetrieveDto, AnamneseDetailDto,
    AnamneseRequestDto,
};
use crate::domain::model::{AnamneseOperation, OperationIdentity};
use crate::infrastructure::controller::dto::SuccessResponseDto;
use crate::infrastructure::controller::extractor::{ApiJson, ApiPath};
use crate::infrastructure::controller::handler::HttpError;
use crate::infrastructure::controller::{ApplicationState, RequestContext};
use axum::Extension;
use axum::extract::State;
use axum::http::StatusCode;
use uuid::Uuid;

pub async fn find_by_patient_and_nucleo(
    State(state): State<ApplicationState>,
    ApiPath((patient_id, nucleo_id)): ApiPath<(Uuid, Uuid)>,
    Extension(context): Extension<RequestContext>,
) -> Result<SuccessResponseDto<AnamneseDetailDto>, HttpError> {
    let data = state
        .acolhimento_anamnese_service
        .find_by_patient_and_nucleo(patient_id, nucleo_id)
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    Ok(SuccessResponseDto::new(
        "ANAMNESE_RETRIEVED",
        context.correlation_id.to_string(),
        "Anamnese recuperada com sucesso",
        data,
    ))
}

pub async fn create_for_nucleo(
    State(state): State<ApplicationState>,
    ApiPath((patient_id, nucleo_id)): ApiPath<(Uuid, Uuid)>,
    Extension(context): Extension<RequestContext>,
    Extension(operation): Extension<OperationIdentity>,
    ApiJson(request): ApiJson<AnamneseRequestDto>,
) -> Result<(StatusCode, SuccessResponseDto<()>), HttpError> {
    state
        .acolhimento_anamnese_service
        .change_nucleo_anamnese(
            patient_id,
            nucleo_id,
            AnamneseOperation::Create,
            Some(request.payload),
            &operation,
        )
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    Ok((
        StatusCode::CREATED,
        SuccessResponseDto::new(
            "ANAMNESE_CREATED",
            context.correlation_id.to_string(),
            "Anamnese registrada com sucesso",
            (),
        ),
    ))
}

pub async fn update_for_nucleo(
    State(state): State<ApplicationState>,
    ApiPath((patient_id, nucleo_id)): ApiPath<(Uuid, Uuid)>,
    Extension(context): Extension<RequestContext>,
    Extension(operation): Extension<OperationIdentity>,
    ApiJson(request): ApiJson<AnamneseRequestDto>,
) -> Result<SuccessResponseDto<()>, HttpError> {
    state
        .acolhimento_anamnese_service
        .change_nucleo_anamnese(
            patient_id,
            nucleo_id,
            AnamneseOperation::Update,
            Some(request.payload),
            &operation,
        )
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    Ok(SuccessResponseDto::new(
        "ANAMNESE_UPDATED",
        context.correlation_id.to_string(),
        "Anamnese atualizada com sucesso",
        (),
    ))
}

pub async fn delete_for_nucleo(
    State(state): State<ApplicationState>,
    ApiPath((patient_id, nucleo_id)): ApiPath<(Uuid, Uuid)>,
    Extension(context): Extension<RequestContext>,
    Extension(operation): Extension<OperationIdentity>,
) -> Result<SuccessResponseDto<()>, HttpError> {
    state
        .acolhimento_anamnese_service
        .change_nucleo_anamnese(
            patient_id,
            nucleo_id,
            AnamneseOperation::Delete,
            None,
            &operation,
        )
        .await
        .map_err(|error| HttpError::new(error, context.clone()))?;
    Ok(SuccessResponseDto::new(
        "ANAMNESE_DELETED",
        context.correlation_id.to_string(),
        "Anamnese removida com sucesso",
        (),
    ))
}

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
    Extension(operation): Extension<OperationIdentity>,
    ApiJson(dto): ApiJson<AcolhimentoAnamneseCreateUpdateDto>,
) -> Result<SuccessResponseDto<AcolhimentoAnamneseRetrieveDto>, HttpError> {
    state
        .acolhimento_anamnese_service
        .upsert(dto, &operation)
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
    Extension(operation): Extension<OperationIdentity>,
    ApiJson(mut dto): ApiJson<AcolhimentoAnamneseCreateUpdateDto>,
) -> Result<SuccessResponseDto<AcolhimentoAnamneseRetrieveDto>, HttpError> {
    dto.patient_id = Some(patient_id);
    state
        .acolhimento_anamnese_service
        .upsert(dto, &operation)
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
