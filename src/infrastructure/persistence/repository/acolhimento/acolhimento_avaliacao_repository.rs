use uuid::Uuid;

use super::{AcolhimentoOperationRepository, NucleoPatientRepository};
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::OperationIdentity;
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
        operation: &OperationIdentity,
    ) -> Result<AcolhimentoAvaliacaoEntity, AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        AcolhimentoOperationRepository::lock(&transaction, operation).await?;
        NucleoPatientRepository::lock_patient(&transaction, entity.patient_id).await?;
        let exists: bool = transaction
            .query_one(
                "SELECT EXISTS(SELECT 1 FROM nucleo_patient WHERE patient_id = $1)",
                &[&entity.patient_id],
            )
            .await
            .map_err(database_error)?
            .try_get(0)
            .map_err(database_error)?;
        if !exists {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }
        let row = transaction
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
        let saved = AcolhimentoAvaliacaoEntity::from_row(&row).map_err(database_error)?;
        AcolhimentoOperationRepository::complete(&transaction, operation).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok(saved)
    }

    pub async fn delete_by_id(
        &self,
        id: Uuid,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        AcolhimentoOperationRepository::lock(&transaction, operation).await?;
        let affected = transaction
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
        AcolhimentoOperationRepository::complete(&transaction, operation).await?;
        transaction.commit().await.map_err(database_error)
    }
}
