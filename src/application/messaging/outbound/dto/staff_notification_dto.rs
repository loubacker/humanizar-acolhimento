use serde::Serialize;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct StaffVinculadoDto {
    pub nucleo_patient_id: Uuid,
    pub patient_id: Uuid,
    pub nucleo_id: Uuid,
    pub staff_user_id: Uuid,
    pub role: String,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct StaffDesvinculadoDto {
    pub nucleo_patient_id: Uuid,
    pub patient_id: Uuid,
    pub nucleo_id: Uuid,
    pub staff_user_id: Uuid,
}
