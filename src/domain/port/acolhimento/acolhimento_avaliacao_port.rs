use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::{AcolhimentoAvaliacao, OperationIdentity};

#[async_trait]
pub trait AcolhimentoAvaliacaoPort: Send + Sync {
    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Vec<AcolhimentoAvaliacao>, AcolhimentoException>;

    async fn create(
        &self,
        model: AcolhimentoAvaliacao,
        operation: &OperationIdentity,
    ) -> Result<AcolhimentoAvaliacao, AcolhimentoException>;

    async fn delete_by_id(
        &self,
        id: Uuid,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException>;
}
