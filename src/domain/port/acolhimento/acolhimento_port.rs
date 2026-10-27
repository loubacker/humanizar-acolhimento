use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::Acolhimento;

#[async_trait]
pub trait AcolhimentoPort: Send + Sync {
    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Option<Acolhimento>, AcolhimentoException>;

    async fn upsert(&self, acolhimento: Acolhimento) -> Result<Acolhimento, AcolhimentoException>;

    async fn delete_by_patient_id(&self, patient_id: Uuid) -> Result<(), AcolhimentoException>;
}
