use async_trait::async_trait;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::DeadLetterMessage;
use crate::domain::port::messaging::DeadLetterQuarantinePort;
use crate::infrastructure::persistence::repository::DeadLetterQuarantineRepository;

pub(crate) struct DeadLetterQuarantineAdapter {
    repository: DeadLetterQuarantineRepository,
}

impl DeadLetterQuarantineAdapter {
    pub fn new(repository: DeadLetterQuarantineRepository) -> Self {
        Self { repository }
    }
}

#[async_trait]
impl DeadLetterQuarantinePort for DeadLetterQuarantineAdapter {
    async fn store(&self, message: &DeadLetterMessage) -> Result<(), AcolhimentoException> {
        self.repository.store(message).await
    }
}
