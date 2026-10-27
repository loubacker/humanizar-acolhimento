use crate::domain::exception::TechnicalError;
use crate::domain::port::messaging::{MessagePublisherPort, OutboxEventPort};
use crate::infrastructure::config::rabbit::RabbitSettings;

use futures_util::{StreamExt, stream};
use std::sync::Arc;
use tokio_util::sync::CancellationToken;
use uuid::Uuid;

pub(crate) async fn run(
    outbox: Arc<dyn OutboxEventPort>,
    publisher: Arc<dyn MessagePublisherPort>,
    settings: Arc<RabbitSettings>,
    stop: CancellationToken,
) -> Result<(), TechnicalError> {
    let owner = Uuid::new_v4();
    loop {
        if !publisher.is_available() {
            return Err(TechnicalError::new("Canal publisher indisponivel"));
        }
        let now = chrono::Local::now().naive_local();
        let lease = chrono::Duration::from_std(settings.lease)
            .map_err(|error| TechnicalError::with_source("Lease invalida", error))?;
        let messages = outbox
            .claim(owner, settings.batch_size, now, now + lease)
            .await
            .map_err(|error| TechnicalError::with_source("Falha no claim outbox", error))?;
        let processor = super::OutboxEventProcessor::new(
            outbox.clone(),
            publisher.clone(),
            owner,
            settings.retry_policy,
        );
        let results = stream::iter(messages)
            .map(|message| processor.process(message))
            .buffer_unordered(settings.parallelism)
            .collect::<Vec<_>>()
            .await;
        for result in results {
            result?;
        }
        if !publisher.is_available() {
            return Err(TechnicalError::new("Canal publisher interrompido"));
        }
        tokio::select! {
            _ = stop.cancelled() => return Ok(()),
            _ = tokio::time::sleep(settings.poll_interval) => {}
        }
    }
}
