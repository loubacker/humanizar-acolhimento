use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::Acolhimento;
use crate::domain::port::AcolhimentoPort;
use crate::infrastructure::persistence::entity::AcolhimentoEntity;
use crate::infrastructure::persistence::repository::AcolhimentoRepository;

pub(crate) struct AcolhimentoAdapter {
    repository: AcolhimentoRepository,
}

impl AcolhimentoAdapter {
    pub fn new(repository: AcolhimentoRepository) -> Self {
        Self { repository }
    }
}

#[async_trait]
impl AcolhimentoPort for AcolhimentoAdapter {
    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Option<Acolhimento>, AcolhimentoException> {
        let opt = self.repository.find_by_patient_id(patient_id).await?;
        Ok(opt.map(|e| {
            Acolhimento::restore(
                e.id,
                e.patient_id,
                e.data_anamnese,
                e.hora_anamnese,
                e.observacoes,
                e.created_at,
                e.updated_at,
            )
        }))
    }

    async fn upsert(&self, acolhimento: Acolhimento) -> Result<Acolhimento, AcolhimentoException> {
        let entity = AcolhimentoEntity {
            id: acolhimento.id(),
            patient_id: acolhimento.patient_id(),
            data_anamnese: acolhimento.data_anamnese(),
            hora_anamnese: acolhimento.hora_anamnese(),
            observacoes: acolhimento.observacoes().map(ToOwned::to_owned),
            created_at: acolhimento.created_at(),
            updated_at: acolhimento.updated_at(),
        };
        let saved = self.repository.upsert(entity).await?;
        Ok(Acolhimento::restore(
            saved.id,
            saved.patient_id,
            saved.data_anamnese,
            saved.hora_anamnese,
            saved.observacoes,
            saved.created_at,
            saved.updated_at,
        ))
    }

    async fn delete_by_patient_id(&self, patient_id: Uuid) -> Result<(), AcolhimentoException> {
        self.repository.delete_by_patient_id(patient_id).await
    }
}
