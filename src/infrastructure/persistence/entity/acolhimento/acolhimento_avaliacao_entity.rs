use chrono::NaiveDateTime;
use uuid::Uuid;

pub(crate) struct AcolhimentoAvaliacaoEntity {
    pub id: Uuid,
    pub patient_id: Uuid,
    pub avaliacao_paciente_id: Uuid,
    pub titulo_avaliacao: String,
    pub modalidade: Option<String>,
    pub status: Option<String>,
    pub created_at: NaiveDateTime,
}

impl AcolhimentoAvaliacaoEntity {
    pub fn from_row(row: &tokio_postgres::Row) -> Result<Self, tokio_postgres::Error> {
        Ok(Self {
            id: row.try_get("id")?,
            patient_id: row.try_get("patient_id")?,
            avaliacao_paciente_id: row.try_get("avaliacao_paciente_id")?,
            titulo_avaliacao: row.try_get("titulo_avaliacao")?,
            modalidade: row.try_get("modalidade")?,
            status: row.try_get("status")?,
            created_at: row.try_get("created_at")?,
        })
    }
}
