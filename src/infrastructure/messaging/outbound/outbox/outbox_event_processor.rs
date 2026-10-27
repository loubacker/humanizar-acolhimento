use super::OutboxRetryPolicy;
use crate::domain::exception::TechnicalError;
use crate::domain::model::messaging::OutboxMessage;
use crate::domain::port::messaging::{MessagePublisherPort, OutboxEventPort};
use std::sync::Arc;
use uuid::Uuid;

pub(crate) struct OutboxEventProcessor {
    outbox: Arc<dyn OutboxEventPort>,
    publisher: Arc<dyn MessagePublisherPort>,
    owner: Uuid,
    retry: OutboxRetryPolicy,
}

impl OutboxEventProcessor {
    pub fn new(
        outbox: Arc<dyn OutboxEventPort>,
        publisher: Arc<dyn MessagePublisherPort>,
        owner: Uuid,
        retry: OutboxRetryPolicy,
    ) -> Self {
        Self {
            outbox,
            publisher,
            owner,
            retry,
        }
    }

    pub async fn process(&self, message: OutboxMessage) -> Result<(), TechnicalError> {
        let event_id = message.event_id();
        let correlation_id = message.correlation_id();
        let persisted = match self.publisher.publish(&message).await {
            Ok(()) => {
                self.outbox
                    .complete(&message, self.owner, chrono::Local::now().naive_local())
                    .await
            }
            Err(error) => {
                tracing::warn!(
                    service = "humanizar-acolhimento",
                    phase = "publish",
                    outcome = "failure",
                    reason_code = error.reason_code().code(),
                    %event_id,
                    %correlation_id,
                );
                let delay = chrono::Duration::from_std(
                    self.retry.delay(message.attempt_count().saturating_add(1)),
                )
                .map_err(|error| TechnicalError::with_source("Delay invalido", error))?;
                self.outbox
                    .fail(
                        &message,
                        self.owner,
                        chrono::Local::now().naive_local() + delay,
                    )
                    .await
            }
        }
        .map_err(|error| {
            TechnicalError::with_source("Falha ao confirmar estado da outbox", error)
        })?;
        if !persisted {
            tracing::warn!(
                service = "humanizar-acolhimento",
                phase = "outbox_complete",
                outcome = "lost_claim",
                %event_id,
                %correlation_id,
            );
        }
        Ok(())
    }
}
