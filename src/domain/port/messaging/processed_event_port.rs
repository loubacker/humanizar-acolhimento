use async_trait::async_trait;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::{
    AcolhimentoRetrieveCommand, ProcessedEvent, ProcessingOutcome,
};

#[async_trait]
pub trait ProcessedEventPort: Send + Sync {
    async fn process(
        &self,
        command: AcolhimentoRetrieveCommand,
    ) -> Result<ProcessingOutcome, AcolhimentoException>;

    async fn record_failure(
        &self,
        event: ProcessedEvent,
    ) -> Result<ProcessingOutcome, AcolhimentoException>;
}
