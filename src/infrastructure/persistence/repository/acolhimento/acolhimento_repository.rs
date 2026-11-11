use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;
use crate::infrastructure::persistence::entity::AcolhimentoEntity;

pub(crate) struct AcolhimentoRepository {
    database: DatabaseConfig,
}

impl AcolhimentoRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        Self { database }
    }

    pub async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Option<AcolhimentoEntity>, AcolhimentoException> {
        let client = self.database.acquire().await?;
        client
            .query_opt(
                r#"
                SELECT
                    id,
                    patient_id,
                    data_anamnese,
                    hora_anamnese,
                    observacoes,
                    created_at,
                    updated_at
                FROM acolhimento
                WHERE patient_id = $1
                "#,
                &[&patient_id],
            )
            .await
            .map_err(database_error)?
            .as_ref()
            .map(AcolhimentoEntity::from_row)
            .transpose()
            .map_err(database_error)
    }
}
