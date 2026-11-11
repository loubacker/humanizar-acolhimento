use serde::Deserialize;
use uuid::Uuid;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AvaliacaoPacienteReferenceEnvelopeDto<T> {
    pub code: String,
    pub success: bool,
    pub correlation_id: Uuid,
    pub data: T,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AvaliacaoPacienteErrorEnvelopeDto {
    pub status: u16,
    pub reason_code: String,
    pub correlation_id: Uuid,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AvaliacaoPacienteReferenceDto {
    pub id: Uuid,
    pub patient_id: Uuid,
    pub titulo_avaliacao: Option<String>,
    pub modalidade: String,
    pub status: String,
}
