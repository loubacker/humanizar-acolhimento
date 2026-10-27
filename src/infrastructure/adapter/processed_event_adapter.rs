use async_trait::async_trait;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::{
    AcolhimentoRetrieveCommand, ProcessedEvent, ProcessingOutcome,
};
use crate::domain::port::messaging::ProcessedEventPort;
use crate::infrastructure::persistence::repository::messaging::ProcessedEventRepository;

pub(crate) struct ProcessedEventAdapter {
    repository: ProcessedEventRepository,
}

impl ProcessedEventAdapter {
    pub fn new(repository: ProcessedEventRepository) -> Self {
        Self { repository }
    }
}

#[async_trait]
impl ProcessedEventPort for ProcessedEventAdapter {
    async fn process(
        &self,
        command: AcolhimentoRetrieveCommand,
    ) -> Result<ProcessingOutcome, AcolhimentoException> {
        self.repository.process(command).await
    }

    async fn record_failure(
        &self,
        event: ProcessedEvent,
    ) -> Result<ProcessingOutcome, AcolhimentoException> {
        self.repository.record_failure(event).await
    }
}
