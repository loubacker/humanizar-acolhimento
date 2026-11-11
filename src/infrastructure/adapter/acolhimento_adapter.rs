use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::Acolhimento;
use crate::domain::port::AcolhimentoPort;
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
}
