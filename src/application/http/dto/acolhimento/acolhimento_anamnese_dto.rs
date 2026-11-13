use chrono::{NaiveDate, NaiveDateTime, NaiveTime};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use uuid::Uuid;

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AnamneseDetailDto {
    pub data_anamnese: NaiveDate,
    pub hora_anamnese: NaiveTime,
    pub observacoes: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct AnamneseRequestDto {
    pub payload: AnamnesePeriodPayloadDto,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AnamnesePeriodPayloadDto {
    pub nucleo_patient_id: Uuid,
    #[serde(flatten)]
    pub detail: AnamneseDetailDto,
}

#[derive(Debug, Clone, Deserialize)]
pub struct AnamneseDeleteRequestDto {
    pub payload: AnamnesePeriodIdentityDto,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AnamnesePeriodIdentityDto {
    pub nucleo_patient_id: Uuid,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AnamneseRetrieveDto {
    pub nucleo_patient_id: Uuid,
    #[serde(flatten)]
    pub detail: AnamneseDetailDto,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AnamneseSectionDto {
    pub nucleo_patient_id: Uuid,
    pub anamnese: Option<Value>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoAnamneseCreateUpdateDto {
    pub patient_id: Option<Uuid>,
    pub data_anamnese: Option<NaiveDate>,
    pub hora_anamnese: Option<NaiveTime>,
    pub observacoes: Option<String>,
    #[serde(default)]
    pub secoes: Vec<AnamneseSectionDto>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AnamneseItemDto {
    pub nucleo_patient_id: Uuid,
    pub nucleo_id: Uuid,
    pub anamnese: Option<Value>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoAnamneseRetrieveDto {
    pub id: Uuid,
    pub patient_id: Uuid,
    pub data_anamnese: Option<NaiveDate>,
    pub hora_anamnese: Option<NaiveTime>,
    pub observacoes: Option<String>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
    pub secoes: Vec<AnamneseItemDto>,
}
