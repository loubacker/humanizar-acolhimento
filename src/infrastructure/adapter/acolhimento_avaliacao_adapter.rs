use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::{AcolhimentoAvaliacao, OperationIdentity};
use crate::domain::port::AcolhimentoAvaliacaoPort;
use crate::infrastructure::persistence::entity::AcolhimentoAvaliacaoEntity;
use crate::infrastructure::persistence::repository::AcolhimentoAvaliacaoRepository;

pub(crate) struct AcolhimentoAvaliacaoAdapter {
    repository: AcolhimentoAvaliacaoRepository,
}

impl AcolhimentoAvaliacaoAdapter {
    pub fn new(repository: AcolhimentoAvaliacaoRepository) -> Self {
        Self { repository }
    }
}

#[async_trait]
impl AcolhimentoAvaliacaoPort for AcolhimentoAvaliacaoAdapter {
    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Vec<AcolhimentoAvaliacao>, AcolhimentoException> {
        let rows = self.repository.find_by_patient_id(patient_id).await?;
        Ok(rows
            .into_iter()
            .map(|e| {
                AcolhimentoAvaliacao::restore(
                    e.id,
                    e.patient_id,
                    e.avaliacao_paciente_id,
                    e.titulo_avaliacao,
                    e.modalidade,
                    e.status,
                    e.created_at,
                )
            })
            .collect())
    }

    async fn create(
        &self,
        model: AcolhimentoAvaliacao,
        operation: &OperationIdentity,
    ) -> Result<AcolhimentoAvaliacao, AcolhimentoException> {
        let entity = AcolhimentoAvaliacaoEntity {
            id: model.id(),
            patient_id: model.patient_id(),
            avaliacao_paciente_id: model.avaliacao_paciente_id(),
            titulo_avaliacao: model.titulo_avaliacao().map(ToOwned::to_owned),
            modalidade: model.modalidade().map(ToOwned::to_owned),
            status: model.status().map(ToOwned::to_owned),
            created_at: model.created_at(),
        };
        let saved = self.repository.create(entity, operation).await?;
        Ok(AcolhimentoAvaliacao::restore(
            saved.id,
            saved.patient_id,
            saved.avaliacao_paciente_id,
            saved.titulo_avaliacao,
            saved.modalidade,
            saved.status,
            saved.created_at,
        ))
    }

    async fn delete_by_id(
        &self,
        id: Uuid,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        self.repository.delete_by_id(id, operation).await
    }
}
