use chrono::{DateTime, NaiveDateTime, Utc};
use serde_json::Value;
use uuid::Uuid;

pub(crate) struct NucleoPatientHistoryEntity {
    pub id: Uuid,
    pub nucleo_id: Uuid,
    pub created_at: Option<NaiveDateTime>,
    pub desvinculado_at: DateTime<Utc>,
    pub desvinculado_por: Uuid,
    pub desvinculo_operation_id: Uuid,
    pub possui_anamnese: bool,
    pub anamnese: Option<Value>,
}

impl NucleoPatientHistoryEntity {
    pub fn from_row(row: &tokio_postgres::Row) -> Result<Self, tokio_postgres::Error> {
        Ok(Self {
            id: row.try_get("id")?,
            nucleo_id: row.try_get("nucleo_id")?,
            created_at: row.try_get("created_at")?,
            desvinculado_at: row.try_get("desvinculado_at")?,
            desvinculado_por: row.try_get("desvinculado_por")?,
            desvinculo_operation_id: row.try_get("desvinculo_operation_id")?,
            possui_anamnese: row.try_get("possui_anamnese")?,
            anamnese: row.try_get("anamnese")?,
        })
    }
}
