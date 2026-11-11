use crate::application::catalog::QueueCatalog;
use crate::application::messaging::inbound::handler::AcolhimentoRetrieveHandler;
use crate::application::messaging::inbound::mapper::AcolhimentoCommandMapper;
use crate::domain::exception::TechnicalError;
use crate::infrastructure::config::rabbit::{RabbitAcknowledgementConfig, rabbit_error};
use futures_util::StreamExt;
use lapin::options::{BasicConsumeOptions, BasicQosOptions};
use lapin::types::FieldTable;
use lapin::{Channel, Consumer};
use std::sync::Arc;
use tokio_util::sync::CancellationToken;

pub(crate) async fn subscribe(
    channel: &Channel,
    prefetch: u16,
    tag: &str,
) -> Result<Consumer, TechnicalError> {
    channel
        .basic_qos(prefetch, BasicQosOptions::default())
        .await
        .map_err(rabbit_error)?;
    channel
        .basic_consume(
            QueueCatalog::PATIENT_RETRIEVE.into(),
            tag.into(),
            BasicConsumeOptions::default(),
            FieldTable::default(),
        )
        .await
        .map_err(rabbit_error)
}

pub(crate) async fn run(
    mut consumer: Consumer,
    handler: Arc<AcolhimentoRetrieveHandler>,
    stop: CancellationToken,
) -> Result<(), TechnicalError> {
    loop {
        let delivery = tokio::select! {
            _ = stop.cancelled() => return Ok(()),
            delivery = consumer.next() => delivery.ok_or_else(|| TechnicalError::new("Consumer RabbitMQ encerrado"))?.map_err(rabbit_error)?,
        };
        let command = AcolhimentoCommandMapper::parse(
            &delivery.data,
            delivery.exchange.as_str(),
            delivery.routing_key.as_str(),
        );
        let mut correlation_id = command.as_ref().ok().map(|value| value.correlation_id);
        let mut event_id = command.as_ref().ok().map(|value| value.event_id);
        let result = match command {
            Ok(command) => handler.handle(command).await.map(|_| ()),
            Err(error) => Err(error),
        };
        match result {
            Ok(()) => {
                RabbitAcknowledgementConfig::ack(&delivery).await?;
                tracing::info!(
                    service = "humanizar-acolhimento",
                    phase = "consume_commit",
                    outcome = "success",
                    ?correlation_id,
                    ?event_id
                );
            }
            Err(error) => {
                let mut retry = error.is_retryable();
                if !retry
                    && error.reason_code()
                        != crate::domain::model::enums::ReasonCode::MessageIdentityMismatch
                    && let Some(event) = AcolhimentoCommandMapper::failed_event(
                        &delivery.data,
                        error.reason_code(),
                        chrono::Utc::now().naive_utc(),
                    )
                {
                    correlation_id = Some(event.correlation_id());
                    event_id = Some(event.event_id());
                    match handler.record_failure(event).await {
                        Ok(crate::domain::model::messaging::ProcessingOutcome::Duplicate) => {
                            RabbitAcknowledgementConfig::ack(&delivery).await?;
                            continue;
                        }
                        Ok(crate::domain::model::messaging::ProcessingOutcome::Processed) => {}
                        Err(record_error) => {
                            retry = record_error.reason_code()
                                != crate::domain::model::enums::ReasonCode::MessageIdentityMismatch;
                        }
                    }
                }
                tracing::warn!(
                    service = "humanizar-acolhimento",
                    phase = "consume",
                    outcome = "failure",
                    reason_code = error.reason_code().code(),
                    ?correlation_id,
                    ?event_id
                );
                {
                    RabbitAcknowledgementConfig::nack(&delivery, retry).await?;
                }
            }
        };
    }
}
