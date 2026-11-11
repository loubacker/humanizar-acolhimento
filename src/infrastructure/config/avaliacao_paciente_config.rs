use url::Url;

use super::EnvironmentConfig;
use crate::domain::exception::TechnicalError;

#[derive(Clone)]
pub struct AvaliacaoPacienteSettings {
    base_url: Url,
}

impl AvaliacaoPacienteSettings {
    pub fn from_env() -> Result<Self, TechnicalError> {
        Self::new(EnvironmentConfig::required("AVALIACAO_PACIENTE_URL")?)
    }

    pub fn new(value: impl AsRef<str>) -> Result<Self, TechnicalError> {
        let base_url = Url::parse(value.as_ref()).map_err(|error| {
            TechnicalError::with_source("AVALIACAO_PACIENTE_URL invalida", error)
        })?;
        if !matches!(base_url.scheme(), "http" | "https")
            || base_url.host_str().is_none()
            || !base_url.username().is_empty()
            || base_url.password().is_some()
            || base_url.query().is_some()
            || base_url.fragment().is_some()
            || !matches!(base_url.path(), "" | "/")
        {
            return Err(TechnicalError::new(
                "AVALIACAO_PACIENTE_URL deve conter somente o endereco do servico",
            ));
        }
        Ok(Self { base_url })
    }

    pub(crate) fn endpoint(&self, id: uuid::Uuid) -> Url {
        let mut url = self.base_url.clone();
        url.set_path(&format!("/api/avaliacao/pacientes/{id}"));
        url
    }

    pub(crate) fn patient_endpoint(&self, patient_id: uuid::Uuid) -> Url {
        let mut url = self.base_url.clone();
        url.set_path(&format!("/api/avaliacao/pacientes/paciente/{patient_id}"));
        url
    }
}
