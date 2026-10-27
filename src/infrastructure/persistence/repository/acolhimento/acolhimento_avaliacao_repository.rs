use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;
use crate::infrastructure::persistence::entity::AcolhimentoAvaliacaoEntity;

pub(crate) struct AcolhimentoAvaliacaoRepository {
    database: DatabaseConfig,
}

impl AcolhimentoAvaliacaoRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        Self { database }
    }

    pub async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Vec<AcolhimentoAvaliacaoEntity>, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let rows = client
            .query(
                r#"
                SELECT
                    id,
                    patient_id,
                    avaliacao_paciente_id,
                    titulo_avaliacao,
                    modalidade,
                    status,
                    created_at
                FROM acolhimento_avaliacao
                WHERE patient_id = $1
                ORDER BY created_at DESC, id DESC
                "#,
                &[&patient_id],
            )
            .await
            .map_err(database_error)?;
        rows.iter()
            .map(AcolhimentoAvaliacaoEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)
    }

    pub async fn create(
        &self,
        entity: AcolhimentoAvaliacaoEntity,
    ) -> Result<AcolhimentoAvaliacaoEntity, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let row = client
            .query_one(
                r#"
                INSERT INTO acolhimento_avaliacao (
                    id,
                    patient_id,
                    avaliacao_paciente_id,
                    titulo_avaliacao,
                    modalidade,
                    status,
                    created_at
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                RETURNING
                    id,
                    patient_id,
                    avaliacao_paciente_id,
                    titulo_avaliacao,
                    modalidade,
                    status,
                    created_at
                "#,
                &[
                    &entity.id,
                    &entity.patient_id,
                    &entity.avaliacao_paciente_id,
                    &entity.titulo_avaliacao,
                    &entity.modalidade,
                    &entity.status,
                    &entity.created_at,
                ],
            )
            .await
            .map_err(database_error)?;
        AcolhimentoAvaliacaoEntity::from_row(&row).map_err(database_error)
    }

    pub async fn delete_by_id(&self, id: Uuid) -> Result<(), AcolhimentoException> {
        let client = self.database.acquire().await?;
        let affected = client
            .execute(
                r#"
                DELETE FROM acolhimento_avaliacao
                WHERE id = $1
                "#,
                &[&id],
            )
            .await
            .map_err(database_error)?;
        if affected == 0 {
            return Err(AcolhimentoException::new(
                ReasonCode::AcolhimentoAvaliacaoNotFound,
            ));
        }
        Ok(())
    }
}
