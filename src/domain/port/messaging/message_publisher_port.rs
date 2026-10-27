use async_trait::async_trait;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::OutboxMessage;

#[async_trait]
pub trait MessagePublisherPort: Send + Sync {
    fn is_available(&self) -> bool;

    async fn publish(&self, message: &OutboxMessage) -> Result<(), AcolhimentoException>;
}
