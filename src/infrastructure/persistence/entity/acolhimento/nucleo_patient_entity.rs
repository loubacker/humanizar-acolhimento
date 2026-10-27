use chrono::NaiveDateTime;
use serde_json::Value;
use uuid::Uuid;

pub(crate) struct NucleoPatientEntity {
    pub id: Uuid,
    pub patient_id: Uuid,
    pub nucleo_id: Uuid,
    pub anamnese: Option<Value>,
    pub created_at: Option<NaiveDateTime>,
    pub updated_at: Option<NaiveDateTime>,
}

impl NucleoPatientEntity {
    pub fn from_row(row: &tokio_postgres::Row) -> Result<Self, tokio_postgres::Error> {
        Ok(Self {
            id: row.try_get("id")?,
            patient_id: row.try_get("patient_id")?,
            nucleo_id: row.try_get("nucleo_id")?,
            anamnese: row.try_get("anamnese")?,
            created_at: row.try_get("created_at")?,
            updated_at: row.try_get("updated_at")?,
        })
    }
}
