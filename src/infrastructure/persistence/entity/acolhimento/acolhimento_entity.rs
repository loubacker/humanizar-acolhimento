use chrono::{NaiveDate, NaiveDateTime, NaiveTime};
use uuid::Uuid;

pub(crate) struct AcolhimentoEntity {
    pub id: Uuid,
    pub patient_id: Uuid,
    pub data_anamnese: Option<NaiveDate>,
    pub hora_anamnese: Option<NaiveTime>,
    pub observacoes: Option<String>,
    pub created_at: NaiveDateTime,
    pub updated_at: NaiveDateTime,
}

impl AcolhimentoEntity {
    pub fn from_row(row: &tokio_postgres::Row) -> Result<Self, tokio_postgres::Error> {
        Ok(Self {
            id: row.try_get("id")?,
            patient_id: row.try_get("patient_id")?,
            data_anamnese: row.try_get("data_anamnese")?,
            hora_anamnese: row.try_get("hora_anamnese")?,
            observacoes: row.try_get("observacoes")?,
            created_at: row.try_get("created_at")?,
            updated_at: row.try_get("updated_at")?,
        })
    }
}
