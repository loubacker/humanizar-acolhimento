use std::collections::HashSet;
use std::time::Duration;

use async_trait::async_trait;
use reqwest::{Client, StatusCode};
use serde::de::DeserializeOwned;
use tokio::time::{Instant, timeout_at};
use url::Url;
use uuid::Uuid;

use crate::application::http::dto::{
    AvaliacaoPacienteErrorEnvelopeDto, AvaliacaoPacienteReferenceDto,
    AvaliacaoPacienteReferenceEnvelopeDto, AvaliacaoPacienteReferencePageDto,
};
use crate::application::http::mapper::AvaliacaoPacienteReferenceMapper;
use crate::application::validator::AvaliacaoPacienteReferenceValidator;
use crate::domain::exception::{AcolhimentoException, TechnicalError};
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{AvaliacaoPacienteReference, DelegatedAccess};
use crate::domain::port::AvaliacaoPacienteReferencePort;
use crate::infrastructure::config::AvaliacaoPacienteSettings;

const REQUEST_BUDGET: Duration = Duration::from_secs(15);
const PAGE_SIZE: u32 = 20;
const RESPONSE_LIMIT_BYTES: usize = 1024 * 1024;

pub struct AvaliacaoPacienteReferenceAdapter {
    settings: AvaliacaoPacienteSettings,
    client: Client,
}

impl AvaliacaoPacienteReferenceAdapter {
    pub fn new(settings: AvaliacaoPacienteSettings) -> Result<Self, TechnicalError> {
        let client = Client::builder()
            .connect_timeout(Duration::from_secs(5))
            .timeout(REQUEST_BUDGET)
            .retry(reqwest::retry::never())
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
        expected_code: &str,
        deadline: Instant,
        observation: &mut AvaliacaoPacienteReferenceObservation,
    ) -> Result<Option<T>, AcolhimentoException> {
        observation.remote_status = None;
        let remaining = deadline.saturating_duration_since(Instant::now());
        if remaining.is_zero() {
            return Err(observation.failure("deadline"));
        }
        let mut response = self
            .client
            .get(url)
            .timeout(remaining)
            .bearer_auth(access.token())
            .header("X-Correlation-ID", observation.correlation_id.to_string())
            .header(reqwest::header::ACCEPT, "application/json")
            .send()
            .await
            .map_err(|error| {
                observation.diagnosed(
                    "request",
                    AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error),
                )
            })?;
        let status = response.status();
        observation.remote_status = Some(status.as_u16());
        match status {
            StatusCode::UNAUTHORIZED => {
                return Err(observation.diagnosed(
                    "authorization_status",
                    AcolhimentoException::new(ReasonCode::AuthenticationRequired),
                ));
            }
            StatusCode::FORBIDDEN => {
                return Err(observation.diagnosed(
                    "authorization_status",
                    AcolhimentoException::new(ReasonCode::AuthorizationDenied),
                ));
            }
            StatusCode::OK | StatusCode::NOT_FOUND => {}
            _ => return Err(observation.failure("response_status")),
        }
        let response_correlation = response
            .headers()
            .get("X-Correlation-ID")
            .and_then(|value| value.to_str().ok())
            .and_then(|value| Uuid::parse_str(value).ok());
        if response_correlation != Some(observation.correlation_id) {
            return Err(observation.failure("response_correlation"));
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
            return Err(observation.failure("content_type"));
        }
        let mut body = Vec::new();
        while let Some(chunk) = response.chunk().await.map_err(|error| {
            observation.diagnosed(
                "response_body",
                AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error),
            )
        })? {
            if body.len().saturating_add(chunk.len()) > RESPONSE_LIMIT_BYTES {
                return Err(observation.failure("response_body_limit"));
            }
            body.extend_from_slice(&chunk);
        }
        if status == StatusCode::NOT_FOUND {
            let error: AvaliacaoPacienteErrorEnvelopeDto =
                serde_json::from_slice(&body).map_err(|error| {
                    observation.diagnosed(
                        "not_found_json",
                        AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error),
                    )
                })?;
            if expected_code == "AVALIACAO_FOUND"
                && error.status == StatusCode::NOT_FOUND.as_u16()
                && error.reason_code == "AVALIACAO_NOT_FOUND"
                && error.correlation_id == observation.correlation_id
            {
                observation.ensure_deadline(deadline)?;
                return Ok(None);
            }
            return Err(observation.failure("not_found_contract"));
        }
        let envelope: AvaliacaoPacienteReferenceEnvelopeDto<T> = serde_json::from_slice(&body)
            .map_err(|error| {
                observation.diagnosed(
                    "response_json",
                    AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error),
                )
            })?;
        if !envelope.success
            || envelope.code != expected_code
            || envelope.correlation_id != observation.correlation_id
        {
            return Err(observation.failure("response_envelope"));
        }
        observation.ensure_deadline(deadline)?;
        Ok(Some(envelope.data))
    }

    async fn read_pages(
        &self,
        patient_id: Uuid,
        access: &DelegatedAccess,
        deadline: Instant,
        observation: &mut AvaliacaoPacienteReferenceObservation,
    ) -> Result<Vec<AvaliacaoPacienteReference>, AcolhimentoException> {
        let mut result = Vec::new();
        let mut seen = HashSet::new();
        let mut requested_page = 0;
        let mut expected_total = None;
        loop {
            observation.page = Some(requested_page);
            let page: AvaliacaoPacienteReferencePageDto = self
                .fetch(
                    self.settings
                        .patient_endpoint(patient_id, requested_page, PAGE_SIZE),
                    access,
                    "AVALIACOES_PATIENT_LISTED",
                    deadline,
                    observation,
                )
                .await?
                .ok_or_else(|| observation.failure("page_absent"))?;
            AvaliacaoPacienteReferenceValidator::validate_page(
                &page,
                requested_page,
                PAGE_SIZE,
                expected_total,
            )
            .map_err(|error| observation.diagnosed("page_metadata", error))?;
            expected_total = Some(page.total);
            for value in page.items {
                AvaliacaoPacienteReferenceValidator::validate_reference(&value, patient_id, None)
                    .map_err(|error| observation.diagnosed("reference_identity", error))?;
                if !seen.insert(value.id) {
                    return Err(observation.failure("duplicate_reference"));
                }
                result.push(AvaliacaoPacienteReferenceMapper::to_domain(value));
            }
            if u64::try_from(result.len()).ok() == expected_total {
                observation.ensure_deadline(deadline)?;
                return Ok(result);
            }
            requested_page = requested_page
                .checked_add(1)
                .ok_or_else(|| observation.failure("page_overflow"))?;
        }
    }
}

#[async_trait]
impl AvaliacaoPacienteReferencePort for AvaliacaoPacienteReferenceAdapter {
    async fn find_by_id(
        &self,
        patient_id: Uuid,
        id: Uuid,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<Option<AvaliacaoPacienteReference>, AcolhimentoException> {
        let mut observation =
            AvaliacaoPacienteReferenceObservation::new(correlation_id, "find_by_id");
        let deadline = observation.started + REQUEST_BUDGET;
        let dto: Option<AvaliacaoPacienteReferenceDto> = timeout_at(
            deadline,
            self.fetch(
                self.settings.endpoint(patient_id, id),
                access,
                "AVALIACAO_FOUND",
                deadline,
                &mut observation,
            ),
        )
        .await
        .map_err(|error| {
            observation.diagnosed(
                "deadline",
                AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error),
            )
        })??;
        dto.map(|value| {
            AvaliacaoPacienteReferenceValidator::validate_reference(&value, patient_id, Some(id))
                .map_err(|error| observation.diagnosed("reference_identity", error))?;
            observation.ensure_deadline(deadline)?;
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
        let mut observation =
            AvaliacaoPacienteReferenceObservation::new(correlation_id, "find_by_patient_id");
        let deadline = observation.started + REQUEST_BUDGET;
        timeout_at(
            deadline,
            self.read_pages(patient_id, access, deadline, &mut observation),
        )
        .await
        .map_err(|error| {
            observation.diagnosed(
                "deadline",
                AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error),
            )
        })?
    }
}

struct AvaliacaoPacienteReferenceObservation {
    correlation_id: Uuid,
    operation: &'static str,
    started: Instant,
    page: Option<u32>,
    remote_status: Option<u16>,
}

impl AvaliacaoPacienteReferenceObservation {
    fn new(correlation_id: Uuid, operation: &'static str) -> Self {
        Self {
            correlation_id,
            operation,
            started: Instant::now(),
            page: None,
            remote_status: None,
        }
    }

    fn ensure_deadline(&self, deadline: Instant) -> Result<(), AcolhimentoException> {
        if Instant::now() >= deadline {
            return Err(self.failure("deadline"));
        }
        Ok(())
    }

    fn failure(&self, phase: &'static str) -> AcolhimentoException {
        self.diagnosed(
            phase,
            AcolhimentoException::new(ReasonCode::IntegrationFailure),
        )
    }

    fn diagnosed(&self, phase: &'static str, error: AcolhimentoException) -> AcolhimentoException {
        tracing::warn!(
            service = "humanizar-acolhimento",
            integration = "avaliacao-paciente",
            operation = self.operation,
            phase,
            outcome = "failed",
            correlation_id = %self.correlation_id,
            duration_ms = self.started.elapsed().as_millis(),
            page = self.page,
            remote_status = self.remote_status,
            reason_code = error.reason_code().code(),
            "Falha ao consultar contrato de avaliacao"
        );
        error
    }
}
