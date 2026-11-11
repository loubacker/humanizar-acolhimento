use async_trait::async_trait;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::DeadLetterMessage;

#[async_trait]
pub trait DeadLetterQuarantinePort: Send + Sync {
    async fn store(&self, message: &DeadLetterMessage) -> Result<(), AcolhimentoException>;
}
