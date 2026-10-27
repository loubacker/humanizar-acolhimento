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

    pub async fn upsert(
        &self,
        entity: AcolhimentoEntity,
    ) -> Result<AcolhimentoEntity, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let row = client
            .query_one(
                r#"
                INSERT INTO acolhimento (
                    id,
                    patient_id,
                    data_anamnese,
                    hora_anamnese,
                    observacoes,
                    created_at,
                    updated_at
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                ON CONFLICT (patient_id)
                DO UPDATE SET
                    data_anamnese = EXCLUDED.data_anamnese,
                    hora_anamnese = EXCLUDED.hora_anamnese,
                    observacoes = EXCLUDED.observacoes,
                    updated_at = EXCLUDED.updated_at
                RETURNING
                    id,
                    patient_id,
                    data_anamnese,
                    hora_anamnese,
                    observacoes,
                    created_at,
                    updated_at
                "#,
                &[
                    &entity.id,
                    &entity.patient_id,
                    &entity.data_anamnese,
                    &entity.hora_anamnese,
                    &entity.observacoes,
                    &entity.created_at,
                    &entity.updated_at,
                ],
            )
            .await
            .map_err(database_error)?;
        AcolhimentoEntity::from_row(&row).map_err(database_error)
    }

    pub async fn delete_by_patient_id(&self, patient_id: Uuid) -> Result<(), AcolhimentoException> {
        let client = self.database.acquire().await?;
        client
            .execute(
                r#"
                DELETE FROM acolhimento
                WHERE patient_id = $1
                "#,
                &[&patient_id],
            )
            .await
            .map_err(database_error)?;
        Ok(())
    }
}
