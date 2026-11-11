use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::{
    OperationAction, OperationIdentity, OperationReservation, OperationStatus,
};
use crate::domain::port::AcolhimentoOperationPort;
use crate::infrastructure::persistence::repository::AcolhimentoOperationRepository;

pub(crate) struct AcolhimentoOperationAdapter {
    repository: AcolhimentoOperationRepository,
}

impl AcolhimentoOperationAdapter {
    pub fn new(repository: AcolhimentoOperationRepository) -> Self {
        Self { repository }
    }
}

#[async_trait]
impl AcolhimentoOperationPort for AcolhimentoOperationAdapter {
    async fn reserve(
        &self,
        identity: &OperationIdentity,
    ) -> Result<OperationReservation, AcolhimentoException> {
        self.repository.reserve(identity).await
    }

    async fn find(
        &self,
        id: Uuid,
        actor: Uuid,
        action: OperationAction,
    ) -> Result<Option<OperationStatus>, AcolhimentoException> {
        self.repository.find(id, actor, action).await
    }

    async fn fail(
        &self,
        identity: &OperationIdentity,
        reason_code: &str,
    ) -> Result<(), AcolhimentoException> {
        self.repository.fail(identity, reason_code).await
    }
}
