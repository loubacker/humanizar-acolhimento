use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NucleoStaffDto {
    pub id: Option<Uuid>,
    pub staff_user_id: Option<Uuid>,
    pub role: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NucleoPatientDto {
    pub id: Option<Uuid>,
    pub nucleo_id: Option<Uuid>,
    #[serde(default)]
    pub nucleo_patient_staff: Vec<NucleoStaffDto>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoCreateDto {
    pub patient_id: Option<Uuid>,
    #[serde(default)]
    pub nucleo_patient: Vec<NucleoPatientDto>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoUpdateDto {
    #[serde(default)]
    pub nucleo_patient: Vec<NucleoPatientDto>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct NucleoPatientRetrieveDto {
    pub id: Uuid,
    pub nucleo_id: Uuid,
    pub nucleo_patient_staff: Vec<NucleoStaffDto>,
    pub anamnese: Option<serde_json::Value>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoRetrieveDto {
    pub patient_id: Uuid,
    pub created_at: Option<NaiveDateTime>,
    pub updated_at: Option<NaiveDateTime>,
    pub nucleos: Vec<NucleoPatientRetrieveDto>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoPatientStatusDto {
    pub patient_id: Uuid,
    pub has_acolhimento: bool,
}
