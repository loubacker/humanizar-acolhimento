use chrono::{DateTime, NaiveDateTime, Utc};
use serde::Serialize;
use serde_json::Value;
use uuid::Uuid;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NucleoPatientHistoryStaffDto {
    pub id: Uuid,
    pub staff_user_id: Uuid,
    pub role: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NucleoPatientHistoryDto {
    pub nucleo_patient_id: Uuid,
    pub nucleo_id: Uuid,
    pub created_at: Option<NaiveDateTime>,
    pub desvinculado_at: DateTime<Utc>,
    pub desvinculado_por: Uuid,
    pub desvinculo_operation_id: Uuid,
    pub possui_anamnese: bool,
    pub responsaveis: Vec<NucleoPatientHistoryStaffDto>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NucleoPatientHistoryDetailDto {
    #[serde(flatten)]
    pub periodo: NucleoPatientHistoryDto,
    pub anamnese: Option<Value>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NucleoPatientHistoryPageDto {
    pub content: Vec<NucleoPatientHistoryDto>,
    pub page: u32,
    pub size: u32,
    pub total_elements: u64,
    pub total_pages: u64,
}
