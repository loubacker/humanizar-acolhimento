use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::{
    OperationAction, OperationIdentity, OperationReservation, OperationStatus,
};

#[async_trait]
pub trait AcolhimentoOperationPort: Send + Sync {
    async fn reserve(
        &self,
        identity: &OperationIdentity,
    ) -> Result<OperationReservation, AcolhimentoException>;

    async fn find(
        &self,
        id: Uuid,
        actor: Uuid,
        action: OperationAction,
    ) -> Result<Option<OperationStatus>, AcolhimentoException>;

    async fn fail(
        &self,
        identity: &OperationIdentity,
        reason_code: &str,
    ) -> Result<(), AcolhimentoException>;
}
