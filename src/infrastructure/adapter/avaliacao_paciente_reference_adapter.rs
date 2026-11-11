use std::collections::HashSet;
use std::time::Duration;

use async_trait::async_trait;
use reqwest::{Client, StatusCode};
use serde::de::DeserializeOwned;
use url::Url;
use uuid::Uuid;

use crate::application::http::dto::{
    AvaliacaoPacienteErrorEnvelopeDto, AvaliacaoPacienteReferenceDto,
    AvaliacaoPacienteReferenceEnvelopeDto,
};
use crate::application::http::mapper::AvaliacaoPacienteReferenceMapper;
use crate::domain::exception::{AcolhimentoException, TechnicalError};
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{AvaliacaoPacienteReference, DelegatedAccess};
use crate::domain::port::AvaliacaoPacienteReferencePort;
use crate::infrastructure::config::AvaliacaoPacienteSettings;

pub(crate) struct AvaliacaoPacienteReferenceAdapter {
    settings: AvaliacaoPacienteSettings,
    client: Client,
}

impl AvaliacaoPacienteReferenceAdapter {
    pub fn new(settings: AvaliacaoPacienteSettings) -> Result<Self, TechnicalError> {
        let client = Client::builder()
            .connect_timeout(Duration::from_secs(5))
            .timeout(Duration::from_secs(15))
            .redirect(reqwest::redirect::Policy::none())
            .build()
            .map_err(|error| {
                TechnicalError::with_source("Falha ao configurar consulta de avaliacao", error)
            })?;
        Ok(Self { settings, client })
    }

    async fn fetch<T: DeserializeOwned>(
        &self,
        url: Url,
        access: &DelegatedAccess,
        correlation_id: Uuid,
        expected_code: &str,
    ) -> Result<Option<T>, AcolhimentoException> {
        let mut response = self
            .client
            .get(url)
            .bearer_auth(access.token())
            .header("X-Correlation-ID", correlation_id.to_string())
            .header(reqwest::header::ACCEPT, "application/json")
            .send()
            .await
            .map_err(|error| {
                AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error)
            })?;
        let status = response.status();
        match status {
            StatusCode::UNAUTHORIZED => {
                return Err(AcolhimentoException::new(
                    ReasonCode::AuthenticationRequired,
                ));
            }
            StatusCode::FORBIDDEN => {
                return Err(AcolhimentoException::new(ReasonCode::AuthorizationDenied));
            }
            status if !status.is_success() && status != StatusCode::NOT_FOUND => {
                return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
            }
            _ => {}
        }
        let content_type = response
            .headers()
            .get(reqwest::header::CONTENT_TYPE)
            .and_then(|value| value.to_str().ok())
            .unwrap_or_default();
        if !content_type
            .split(';')
            .next()
            .is_some_and(|value| value.trim().eq_ignore_ascii_case("application/json"))
        {
            return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
        }
        let mut body = Vec::new();
        while let Some(chunk) = response.chunk().await.map_err(|error| {
            AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error)
        })? {
            if body.len().saturating_add(chunk.len()) > 1024 * 1024 {
                return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
            }
            body.extend_from_slice(&chunk);
        }
        if status == StatusCode::NOT_FOUND {
            let error: AvaliacaoPacienteErrorEnvelopeDto =
                serde_json::from_slice(&body).map_err(|error| {
                    AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error)
                })?;
            if expected_code == "AVALIACAO_FOUND"
                && error.status == StatusCode::NOT_FOUND.as_u16()
                && error.reason_code == "AVALIACAO_NOT_FOUND"
                && error.correlation_id == correlation_id
            {
                return Ok(None);
            }
            return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
        }
        let envelope: AvaliacaoPacienteReferenceEnvelopeDto<T> = serde_json::from_slice(&body)
            .map_err(|error| {
                AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error)
            })?;
        if !envelope.success
            || envelope.code != expected_code
            || envelope.correlation_id != correlation_id
        {
            return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
        }
        Ok(Some(envelope.data))
    }
}

#[async_trait]
impl AvaliacaoPacienteReferencePort for AvaliacaoPacienteReferenceAdapter {
    async fn find_by_id(
        &self,
        id: Uuid,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<Option<AvaliacaoPacienteReference>, AcolhimentoException> {
        let dto: Option<AvaliacaoPacienteReferenceDto> = self
            .fetch(
                self.settings.endpoint(id),
                access,
                correlation_id,
                "AVALIACAO_FOUND",
            )
            .await?;
        dto.map(|value| {
            validate(&value)?;
            if value.id != id {
                return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
            }
            Ok(AvaliacaoPacienteReferenceMapper::to_domain(value))
        })
        .transpose()
    }

    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<Vec<AvaliacaoPacienteReference>, AcolhimentoException> {
        let values: Vec<AvaliacaoPacienteReferenceDto> = self
            .fetch(
                self.settings.patient_endpoint(patient_id),
                access,
                correlation_id,
                "AVALIACOES_PATIENT_LISTED",
            )
            .await?
            .ok_or_else(|| AcolhimentoException::new(ReasonCode::AvaliacaoPacienteNotFound))?;
        let mut seen = HashSet::new();
        values
            .into_iter()
            .map(|value| {
                validate(&value)?;
                if value.patient_id != patient_id || !seen.insert(value.id) {
                    return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
                }
                Ok(AvaliacaoPacienteReferenceMapper::to_domain(value))
            })
            .collect()
    }
}

fn validate(dto: &AvaliacaoPacienteReferenceDto) -> Result<(), AcolhimentoException> {
    if dto.id.is_nil()
        || dto.patient_id.is_nil()
        || dto
            .titulo_avaliacao
            .as_ref()
            .is_some_and(|value| value.chars().count() > 255)
        || dto.modalidade.trim().is_empty()
        || dto.modalidade.chars().count() > 50
        || dto.status.trim().is_empty()
        || dto.status.chars().count() > 50
    {
        return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
    }
    Ok(())
}
