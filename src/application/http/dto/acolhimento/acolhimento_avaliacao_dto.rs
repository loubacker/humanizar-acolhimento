use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoAvaliacaoRegisterDto {
    pub patient_id: Option<Uuid>,
    pub avaliacao_paciente_id: Option<Uuid>,
    pub titulo_avaliacao: Option<String>,
    pub modalidade: Option<String>,
    pub status: Option<String>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoAvaliacaoRetrieveDto {
    pub id: Uuid,
    pub patient_id: Uuid,
    pub avaliacao_paciente_id: Uuid,
    pub titulo_avaliacao: String,
    pub modalidade: Option<String>,
    pub status: Option<String>,
    pub created_at: NaiveDateTime,
}
