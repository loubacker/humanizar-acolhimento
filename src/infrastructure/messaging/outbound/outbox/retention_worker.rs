use crate::domain::exception::TechnicalError;
use crate::domain::port::messaging::OutboxEventPort;
use crate::infrastructure::config::rabbit::RabbitSettings;

use std::sync::Arc;
use tokio_util::sync::CancellationToken;

pub(crate) async fn run(
    outbox: Arc<dyn OutboxEventPort>,
    settings: Arc<RabbitSettings>,
    stop: CancellationToken,
) -> Result<(), TechnicalError> {
    loop {
        tokio::select! {
            _ = stop.cancelled() => return Ok(()),
            _ = tokio::time::sleep(settings.retention_interval) => {}
        }
        let now = chrono::Local::now().naive_local();
        let removed = outbox
            .purge(
                now - settings.outbox_retention,
                now - settings.processed_retention,
            )
            .await
            .map_err(|error| TechnicalError::with_source("Falha na retencao", error))?;
        tracing::info!(
            service = "humanizar-acolhimento",
            phase = "retention",
            outcome = "success",
            removed
        );
    }
}
