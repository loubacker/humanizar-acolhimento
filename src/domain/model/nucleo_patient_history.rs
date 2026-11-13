use chrono::{DateTime, NaiveDateTime, Utc};
use serde_json::Value;
use uuid::Uuid;

use super::NucleoPatientStaff;

pub struct NucleoPatientHistory {
    pub id: Uuid,
    pub nucleo_id: Uuid,
    pub created_at: Option<NaiveDateTime>,
    pub desvinculado_at: DateTime<Utc>,
    pub desvinculado_por: Uuid,
    pub desvinculo_operation_id: Uuid,
    pub possui_anamnese: bool,
    pub anamnese: Option<Value>,
    pub responsaveis: Vec<NucleoPatientStaff>,
}

pub struct NucleoPatientHistoryPage {
    pub content: Vec<NucleoPatientHistory>,
    pub page: u32,
    pub size: u32,
    pub total_elements: u64,
}
