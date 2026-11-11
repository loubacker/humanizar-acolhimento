use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::{AvaliacaoPacienteReference, DelegatedAccess};

#[async_trait]
pub trait AvaliacaoPacienteReferencePort: Send + Sync {
    async fn find_by_id(
        &self,
        id: Uuid,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<Option<AvaliacaoPacienteReference>, AcolhimentoException>;

    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<Vec<AvaliacaoPacienteReference>, AcolhimentoException>;
}
