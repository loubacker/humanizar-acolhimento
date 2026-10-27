use std::sync::Arc;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::{
    AcolhimentoRetrieveCommand, ProcessedEvent, ProcessingOutcome,
};
use crate::domain::port::messaging::ProcessedEventPort;

pub struct AcolhimentoRetrieveHandler {
    processed: Arc<dyn ProcessedEventPort>,
}

impl AcolhimentoRetrieveHandler {
    pub fn new(processed: Arc<dyn ProcessedEventPort>) -> Self {
        Self { processed }
    }

    pub async fn handle(
        &self,
        command: AcolhimentoRetrieveCommand,
    ) -> Result<ProcessingOutcome, AcolhimentoException> {
        self.processed.process(command).await
    }

    pub async fn record_failure(
        &self,
        event: ProcessedEvent,
    ) -> Result<ProcessingOutcome, AcolhimentoException> {
        self.processed.record_failure(event).await
    }
}
